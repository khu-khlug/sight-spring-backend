package com.sight.service

import com.sight.repository.FileUploadRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Service
class FileUploadCheckService(
    private val uploads: FileUploadRepository,
    private val storage: StorageService,
    private val deletions: FileUploadDeletionService,
    @Qualifier("fileUploadTaskScheduler") private val scheduler: TaskScheduler,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val transaction =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    fun scheduleUploadCheck(
        fileId: String,
        at: Instant,
    ) {
        check(
            TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive(),
        ) {
            "업로드 확인 예약에는 파일을 저장하는 트랜잭션이 필요합니다."
        }
        val task =
            checkNotNull(scheduler.schedule({ checkUpload(fileId) }, at)) {
                "업로드 확인을 예약할 수 없습니다."
            }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) task.cancel(false)
                }
            },
        )
    }

    private fun checkUpload(fileId: String) {
        try {
            transaction.executeWithoutResult {
                val file = uploads.findForUpdateById(fileId) ?: return@executeWithoutResult
                if (file.isDeleted || file.isUploaded || file.uploadCheckAt.isAfter(Instant.now())) return@executeWithoutResult
                val exists =
                    try {
                        storage.isFileExists(file.fileKey)
                    } catch (e: StorageOperationException) {
                        if (e.reason != StorageFailureReason.NO_SUCH_KEY) throw e
                        false
                    }
                if (!exists) deletions.deleteFileUpload(fileId)
            }
        } catch (e: Exception) {
            logger.error("업로드 확인 실패: fileId={}", fileId, e)
        }
    }
}
