package com.sight.service

import com.sight.repository.FileUploadMaintenanceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate

enum class FileUploadDeletionResult {
    DELETED,
    ALREADY_ABSENT,
    SKIPPED,
}

@Service
class FileUploadLifecycleService(
    private val uploads: FileUploadMaintenanceRepository,
    private val storage: StorageService,
    @Qualifier("fileUploadDeletionExecutor") private val executor: TaskExecutor,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val transaction =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleDeleted(event: FileUploadDeletedEvent) {
        try {
            executor.execute { deleteMarkedUpload(event.fileUploadId) }
        } catch (e: Exception) {
            logger.error("업로드 파일 삭제 작업 등록 실패: fileId={}", event.fileUploadId, e)
        }
    }

    fun deleteMarkedUpload(id: String) {
        try {
            deleteMarkedUploadForMaintenance(id)
        } catch (e: Exception) {
            logger.error("업로드 파일 삭제 실패: fileId={}", id, e)
        }
    }

    fun deleteMarkedUploadForMaintenance(id: String): FileUploadDeletionResult {
        val key =
            transaction.execute {
                uploads.findById(id)?.takeIf { it.isDeleted && !uploads.hasOwnerReferences(id) }?.fileKey
            } ?: return FileUploadDeletionResult.SKIPPED
        var result = FileUploadDeletionResult.DELETED
        try {
            storage.deleteFile(key)
        } catch (e: StorageOperationException) {
            if (e.reason != StorageFailureReason.NO_SUCH_KEY) throw e
            result = FileUploadDeletionResult.ALREADY_ABSENT
        }
        transaction.executeWithoutResult {
            val file = uploads.findLockedById(id)
            if (file != null && file.isDeleted && !uploads.hasOwnerReferences(id)) {
                uploads.deleteUpload(file)
            }
        }
        return result
    }
}
