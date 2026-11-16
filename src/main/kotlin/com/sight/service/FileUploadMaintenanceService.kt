package com.sight.service

import com.sight.domain.file.FileUploadPurpose
import com.sight.repository.FileUploadMaintenanceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.ZoneOffset

@Service
class FileUploadMaintenanceService(
    private val uploads: FileUploadMaintenanceRepository,
    private val storage: StorageService,
    private val lifecycle: FileUploadLifecycleService,
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
                uploads.findById(id)?.takeIf {
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
                val file = uploads.findLockedById(id) ?: return@execute false
                if (file.isDeleted || file.isUploaded || file.uploadCheckedAt != null ||
                    file.uploadCheckAt.isAfter(Instant.now())
                ) {
                    return@execute false
                }
                val purpose = FileUploadPurpose.fromApiPath(file.apiPath)
                if (exists) {
                    if (purpose == FileUploadPurpose.RECORD_ATTACHMENT) file.markUploaded()
                    file.markUploadChecked(Instant.now())
                    false
                } else {
                    if (uploads.hasSingleUseOwnerReferences(id)) return@execute false
                    if (purpose != FileUploadPurpose.RECORD_ATTACHMENT && uploads.hasRecordLinks(id)) return@execute false
                    if (purpose == FileUploadPurpose.RECORD_ATTACHMENT) uploads.deleteRecordLinks(id)
                    file.markDeleted(Instant.now())
                    true
                }
            } == true
        return if (marked) lifecycle.deleteMarkedUploadForMaintenance(id) else FileUploadDeletionResult.SKIPPED
    }

    fun cleanupUpload(
        id: String,
        at: Instant,
    ): FileUploadDeletionResult {
        val marked =
            transaction.execute {
                val file = uploads.findLockedById(id) ?: return@execute false
                val purpose = FileUploadPurpose.fromApiPath(file.apiPath)
                val hasLinks = purpose == FileUploadPurpose.RECORD_ATTACHMENT && uploads.hasRecordLinks(id)
                if (file.deletedAt?.isBefore(at.atOffset(ZoneOffset.UTC).minusMonths(2).toInstant()) == true) {
                    logger.warn("장기 미삭제 업로드 파일: fileId={}, key={}, deletedAt={}", id, file.fileKey, file.deletedAt)
                }
                if (file.isDeleted) return@execute !uploads.hasOwnerReferences(id)
                if (purpose.isInUse(file.isUploaded, hasLinks)) return@execute false
                if (uploads.hasOwnerReferences(id)) return@execute false
                if (purpose == FileUploadPurpose.RECORD_ATTACHMENT) {
                    uploads.markUnreferencedUploadDeleted(id, at)
                } else {
                    uploads.markUnusedUploadDeleted(id, at)
                }
            } == true
        return if (marked) lifecycle.deleteMarkedUploadForMaintenance(id) else FileUploadDeletionResult.SKIPPED
    }
}
