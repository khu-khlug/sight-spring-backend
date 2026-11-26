package com.sight.service

import com.sight.domain.file.FileUpload
import com.sight.repository.FileReferenceRepository
import com.sight.repository.FileUploadRepository
import org.slf4j.LoggerFactory
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
    registry: FileCleanupRegistry,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val bindings = registry.bindings.associateBy { it.referenceType }
    private val transaction =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    init {
        require(bindings.size == registry.bindings.size) { "파일 정리 판단 함수의 참조 종류가 중복되었습니다." }
    }

    fun cleanupUpload(
        id: String,
        at: Instant,
    ): FileUploadDeletionResult {
        val pendingKey =
            transaction.execute {
                val file = uploads.findById(id).orElse(null) ?: return@execute null
                if (!file.isDeleted && !file.isUploaded && !file.uploadCheckAt.isAfter(at) && references.existsByFileUploadId(id)) {
                    file.fileKey
                } else {
                    null
                }
            }
        val objectExists =
            if (pendingKey == null) {
                null
            } else {
                try {
                    storage.isFileExists(pendingKey)
                } catch (e: StorageOperationException) {
                    if (e.reason != StorageFailureReason.NO_SUCH_KEY) throw e
                    false
                }
            }
        val marked =
            transaction.execute {
                val file = uploads.findForUpdateById(id) ?: return@execute false
                if (file.deletedAt?.isBefore(at.atOffset(ZoneOffset.UTC).minusMonths(2).toInstant()) == true) {
                    logger.warn("장기 미삭제 업로드 파일: fileId={}, key={}, deletedAt={}", id, file.fileKey, file.deletedAt)
                }
                if (!file.isDeleted && file.uploadCheckAt.isAfter(at)) return@execute false
                deleteUnusedReferences(file, objectExists)
                if (references.existsByFileUploadId(id)) return@execute false
                if (file.isDeleted) return@execute true
                val changed = uploads.markUnreferencedForDeletion(id, at) == 1
                if (changed) file.markDeleted(at)
                changed
            } == true
        return if (marked) deletions.deleteMarkedFileUploadForMaintenance(id) else FileUploadDeletionResult.SKIPPED
    }

    private fun deleteUnusedReferences(
        file: FileUpload,
        objectExists: Boolean?,
    ) {
        var cursor = ""
        while (true) {
            val batch = references.findByFileUploadIdOrderById(file.id, cursor, REFERENCE_BATCH_SIZE)
            if (batch.isEmpty()) return
            batch.forEach { reference ->
                // 등록하지 않은 도메인의 참조는 사용 중인 것으로 보호한다.
                val binding = bindings[reference.referenceType] ?: return@forEach
                if (binding.isCleanupTarget(FileCleanupContext(file, reference, objectExists))) {
                    binding.releaseDetails?.invoke(reference)
                    references.deleteByIdAndRefTypeAndReferrer(reference.id, reference.referenceType, reference.referrerId)
                }
            }
            cursor = batch.last().id
            if (batch.size < REFERENCE_BATCH_SIZE) return
        }
    }

    companion object {
        private const val REFERENCE_BATCH_SIZE = 100
    }
}
