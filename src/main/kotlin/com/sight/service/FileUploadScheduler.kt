package com.sight.service

import com.sight.repository.FileMaintenanceLockRepository
import com.sight.repository.FileUploadMaintenanceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
@ConditionalOnProperty(name = ["file-upload.maintenance.enabled"], havingValue = "true")
class FileUploadScheduler(
    private val uploads: FileUploadMaintenanceRepository,
    private val locks: FileMaintenanceLockRepository,
    private val maintenance: FileUploadMaintenanceService,
    @Qualifier("fileUploadTaskScheduler") private val scheduler: TaskScheduler,
    transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val reserved = ConcurrentHashMap.newKeySet<String>()
    private val readTransaction =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            isReadOnly = true
        }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCreated(event: FileUploadCreatedEvent) {
        scheduleUploadCheck(event.fileUploadId, event.uploadCheckAt)
    }

    fun scheduleUploadCheck(
        id: String,
        at: Instant,
    ) {
        if (at.isAfter(Instant.now().plusSeconds(60)) || !reserve(id)) return
        try {
            val task =
                scheduler.schedule({
                    checkReservedUpload(id)
                }, at)
            if (task == null) reserved.remove(id)
        } catch (e: Exception) {
            reserved.remove(id)
            logger.error("업로드 확인 예약 실패: fileId={}", id, e)
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun restoreAtStartup() {
        scheduler.schedule({ restoreUploadChecks() }, Instant.now())
    }

    // DB 예약 시각을 기준으로 누락되거나 실행에 실패한 예약을 복구한다.
    @Scheduled(fixedDelay = 60_000)
    fun restoreUploadChecks() {
        try {
            var cursor = ""
            val horizon = Instant.now().plusSeconds(60)
            while (true) {
                val batch =
                    readTransaction.execute {
                        uploads.findPendingReservationsAfter(cursor, horizon, BATCH_SIZE)
                    } ?: return
                cursor = batch.lastOrNull()?.id ?: return
                batch.forEach { (id, at) ->
                    if (at.isAfter(Instant.now())) {
                        scheduleUploadCheck(id, at)
                    } else if (reserve(id)) {
                        checkReservedUpload(id, stopOnConfigurationFailure = true)
                    }
                }
                if (batch.size < BATCH_SIZE) return
            }
        } catch (e: Exception) {
            logger.error("업로드 확인 예약 복구 실패", e)
        }
    }

    @Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Seoul")
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
                        val key = readTransaction.execute { uploads.findById(id)?.fileKey }
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
        private const val MAX_RESERVED = 1000
    }

    private fun reserve(id: String): Boolean =
        synchronized(reserved) {
            reserved.size < MAX_RESERVED && reserved.add(id)
        }

    private fun checkReservedUpload(
        id: String,
        stopOnConfigurationFailure: Boolean = false,
    ) {
        try {
            maintenance.checkUpload(id)
        } catch (e: Exception) {
            logger.error("업로드 확인 실패: fileId={}", id, e)
            if (stopOnConfigurationFailure && e is StorageOperationException && e.stopsMaintenance) throw e
        } finally {
            reserved.remove(id)
        }
    }
}
