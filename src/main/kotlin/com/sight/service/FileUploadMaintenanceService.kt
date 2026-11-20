package com.sight.service

import com.sight.domain.file.FileUploadMissingEvent
import com.sight.domain.file.FileUploadPurpose
import com.sight.repository.FileReferenceRepository
import com.sight.repository.FileUploadRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.ZoneOffset

@Service
class FileUploadMaintenanceService(
    private val uploads: FileUploadRepository,
    private val references: FileReferenceRepository,
    private val storage: StorageService,
    private val deletions: FileUploadDeletionService,
    private val events: ApplicationEventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val transaction =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    fun checkUpload(id: String): FileUploadDeletionResult {
        val key =
            transaction.execute {
                uploads.findById(id).orElse(null)?.takeIf {
                    !it.isDeleted && !it.isUploaded && it.uploadCheckedAt == null &&
                        !it.uploadCheckAt.isAfter(Instant.now())
                }?.fileKey
            } ?: return FileUploadDeletionResult.SKIPPED
        val exists =
            try {
                storage.isFileExists(key)
            } catch (e: StorageOperationException) {
                if (e.reason != StorageFailureReason.NO_SUCH_KEY) throw e
                false
            }
        val marked =
            transaction.execute {
                val file = uploads.findForUpdateById(id) ?: return@execute false
                val at = Instant.now()
                if (file.isDeleted || file.isUploaded || file.uploadCheckedAt != null || file.uploadCheckAt.isAfter(at)) {
                    return@execute false
                }
                if (exists) {
                    if (FileUploadPurpose.fromApiPath(file.apiPath) == FileUploadPurpose.RECORD_ATTACHMENT) file.markUploaded()
                    file.markUploadChecked(at)
                    false
                } else {
                    // 부가 행은 각 도메인이 같은 트랜잭션에서 해제하며, 남은 공통 참조는 보호한다.
                    events.publishEvent(FileUploadMissingEvent(id))
                    if (references.existsByFileUploadId(id)) return@execute false
                    val changed = uploads.markUnreferencedForDeletion(id, at) == 1
                    if (changed) file.markDeleted(at)
                    changed
                }
            } == true
        return if (marked) deletions.deleteMarkedFileUploadForMaintenance(id) else FileUploadDeletionResult.SKIPPED
    }

    fun cleanupUpload(
        id: String,
        at: Instant,
    ): FileUploadDeletionResult {
        val marked =
            transaction.execute {
                val file = uploads.findForUpdateById(id) ?: return@execute false
                if (file.deletedAt?.isBefore(at.atOffset(ZoneOffset.UTC).minusMonths(2).toInstant()) == true) {
                    logger.warn("장기 미삭제 업로드 파일: fileId={}, key={}, deletedAt={}", id, file.fileKey, file.deletedAt)
                }
                if (references.existsByFileUploadId(id)) return@execute false
                if (file.isDeleted) return@execute true
                if (file.uploadCheckAt.isAfter(at)) return@execute false
                val changed = uploads.markUnreferencedForDeletion(id, at) == 1
                if (changed) file.markDeleted(at)
                changed
            } == true
        return if (marked) deletions.deleteMarkedFileUploadForMaintenance(id) else FileUploadDeletionResult.SKIPPED
    }
}
