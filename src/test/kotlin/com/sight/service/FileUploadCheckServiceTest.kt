package com.sight.service

import com.sight.domain.file.testFileUpload
import com.sight.repository.FileUploadRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.scheduling.TaskScheduler
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.concurrent.ScheduledFuture

class FileUploadCheckServiceTest {
    private val uploads = mock<FileUploadRepository>()
    private val storage = mock<StorageService>()
    private val deletions = mock<FileUploadDeletionService>()
    private val scheduler = mock<TaskScheduler>()
    private val transactions = mock<PlatformTransactionManager>()
    private val future = mock<ScheduledFuture<*>>()
    private val file = testFileUpload(createdAt = Instant.EPOCH)
    private lateinit var task: Runnable
    private val service = FileUploadCheckService(uploads, storage, deletions, scheduler, transactions)

    @BeforeEach
    fun `발급 트랜잭션과 일회성 예약을 준비한다`() {
        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(true)
        given(transactions.getTransaction(any())).willAnswer { SimpleTransactionStatus() }
        given(scheduler.schedule(any<Runnable>(), any<Instant>())).willAnswer {
            task = it.getArgument(0)
            future
        }
        given(uploads.findForUpdateById(file.id)).willReturn(file)
    }

    @AfterEach
    fun `트랜잭션 상태를 해제한다`() {
        TransactionSynchronizationManager.clearSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(false)
    }

    @Test
    fun `발급이 롤백되면 예약을 취소한다`() {
        service.scheduleUploadCheck(file.id, file.uploadCheckAt)

        TransactionSynchronizationManager.getSynchronizations().forEach {
            it.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        }

        verify(future).cancel(false)
        verifyNoInteractions(storage, deletions)
    }

    @Test
    fun `미완료 업로드의 객체가 없으면 공통 삭제 서비스로 정리한다`() {
        service.scheduleUploadCheck(file.id, file.uploadCheckAt)

        task.run()

        verify(deletions).deleteFileUpload(file.id)
    }

    @Test
    fun `제출 전 파일의 객체가 있어도 제출 완료로 표시하지 않는다`() {
        given(storage.isFileExists(file.fileKey)).willReturn(true)
        service.scheduleUploadCheck(file.id, file.uploadCheckAt)

        task.run()

        assertFalse(file.isUploaded)
        verifyNoInteractions(deletions)
    }

    @Test
    fun `R2 확인 실패는 파일 삭제로 이어지지 않는다`() {
        given(storage.isFileExists(file.fileKey)).willThrow(StorageOperationException(StorageFailureReason.TRANSIENT, "일시적인 오류입니다."))
        service.scheduleUploadCheck(file.id, file.uploadCheckAt)

        task.run()

        verifyNoInteractions(deletions)
        verify(transactions).rollback(any())
    }
}
