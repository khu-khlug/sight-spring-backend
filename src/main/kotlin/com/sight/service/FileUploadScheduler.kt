package com.sight.service

import com.sight.repository.FileMaintenanceLockRepository
import com.sight.repository.FileUploadMaintenanceRepository
import com.sight.repository.FileUploadRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Service
@ConditionalOnProperty(name = ["file-upload.maintenance.enabled"], havingValue = "true")
class FileUploadScheduler(
    private val uploads: FileUploadMaintenanceRepository,
    private val files: FileUploadRepository,
    private val locks: FileMaintenanceLockRepository,
    private val maintenance: FileUploadMaintenanceService,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val readTransaction =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            isReadOnly = true
        }

    @Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Seoul", scheduler = "fileUploadTaskScheduler")
    fun cleanupMonthly() {
        try {
            val acquired = locks.withLock(CLEANUP_LOCK) { cleanupBatches() }
            if (!acquired) logger.info("업로드 파일 청소 생략: 다른 서버에서 실행 중입니다.")
        } catch (e: Exception) {
            logger.error("업로드 파일 청소 중단", e)
        }
    }

    private fun cleanupBatches() {
        val startedAt = Instant.now()
        var cursor = ""
        var deleted = 0
        var absent = 0
        var failed = 0
        var skipped = 0
        logger.info("업로드 파일 청소 시작: at={}", startedAt)
        try {
            while (true) {
                val ids =
                    readTransaction.execute {
                        uploads.findCleanupCandidatesAfter(cursor, startedAt, BATCH_SIZE)
                    }.orEmpty()
                if (ids.isEmpty()) break
                for (id in ids) {
                    try {
                        when (maintenance.cleanupUpload(id, startedAt)) {
                            FileUploadDeletionResult.DELETED -> deleted++
                            FileUploadDeletionResult.ALREADY_ABSENT -> absent++
                            FileUploadDeletionResult.SKIPPED -> skipped++
                        }
                    } catch (e: Exception) {
                        failed++
                        val key = readTransaction.execute { files.findById(id).orElse(null)?.fileKey }
                        logger.error("업로드 파일 청소 실패: fileId={}, key={}", id, key, e)
                        if (e is StorageOperationException && e.stopsMaintenance) throw e
                    }
                }
                cursor = ids.last()
                if (ids.size < BATCH_SIZE) break
            }
        } finally {
            logger.info(
                "업로드 파일 청소 종료: startedAt={}, endedAt={}, deleted={}, absent={}, failed={}, skipped={}",
                startedAt,
                Instant.now(),
                deleted,
                absent,
                failed,
                skipped,
            )
        }
    }

    companion object {
        private const val BATCH_SIZE = 100
        private const val CLEANUP_LOCK = "sight:file-upload-cleanup"
    }
}
