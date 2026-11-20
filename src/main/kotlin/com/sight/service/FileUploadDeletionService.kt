package com.sight.service

import com.sight.repository.FileReferenceRepository
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

data class FileUploadDeletionRequested(val fileUploadId: String)

@Service
class FileUploadDeletionService(
    private val uploads: FileUploadRepository,
    private val references: FileReferenceRepository,
    private val groupFiles: GroupFileRepository,
    private val storage: StorageService,
    private val events: ApplicationEventPublisher,
    @Qualifier("fileDeletionExecutor") private val executor: TaskExecutor,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val transaction =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    @Transactional
    fun deleteFileUpload(fileUploadId: String) {
        val file = uploads.findForUpdateById(fileUploadId) ?: return
        val at = Instant.now()
        val marked = !file.isDeleted && uploads.markUnreferencedForDeletion(fileUploadId, at) == 1
        if (marked) file.markDeleted(at)
        if (file.isDeleted) {
            events.publishEvent(FileUploadDeletionRequested(fileUploadId))
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleDeletionRequested(event: FileUploadDeletionRequested) {
        try {
            executor.execute { deleteMarkedFileUpload(event.fileUploadId) }
        } catch (e: Exception) {
            logger.error("파일 삭제 작업 등록 실패: fileId={}", event.fileUploadId, e)
        }
    }

    fun deleteMarkedFileUpload(fileUploadId: String) {
        try {
            deleteMarkedFileUploadForMaintenance(fileUploadId)
        } catch (e: Exception) {
            logger.error("파일 삭제 실패: fileId={}", fileUploadId, e)
        }
    }

    fun deleteMarkedFileUploadForMaintenance(fileUploadId: String): FileUploadDeletionResult {
        val key =
            transaction.execute {
                uploads.findById(fileUploadId).orElse(null)
                    ?.takeIf { it.isDeleted && !references.existsByFileUploadId(fileUploadId) }?.fileKey
            } ?: return FileUploadDeletionResult.SKIPPED
        var result = FileUploadDeletionResult.DELETED
        try {
            storage.deleteFile(key)
        } catch (e: StorageOperationException) {
            if (e.reason != StorageFailureReason.NO_SUCH_KEY) throw e
            result = FileUploadDeletionResult.ALREADY_ABSENT
        }
        transaction.executeWithoutResult {
            val file = uploads.findForUpdateById(fileUploadId)
            if (file != null && file.isDeleted && !references.existsByFileUploadId(fileUploadId)) {
                groupFiles.deleteByFileUploadId(fileUploadId)
                uploads.delete(file)
            }
        }
        return result
    }
}
