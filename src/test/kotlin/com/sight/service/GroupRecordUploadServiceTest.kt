package com.sight.service

import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupRecordFile
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupRecordFileRepository
import com.sight.repository.dto.GroupRecordFileView
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.scheduling.TaskScheduler
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture

class GroupRecordUploadServiceTest {
    private val uploads = mock<FileUploadRepository>()
    private val links = mock<GroupRecordFileRepository>()
    private val storage = mock<StorageService>()
    private val references = mock<FileReferenceService>()
    private val deletions = mock<FileUploadDeletionService>()
    private val scheduler = mock<TaskScheduler>()
    private val transactions = mock<PlatformTransactionManager>()
    private val future = mock<ScheduledFuture<*>>()
    private lateinit var task: Runnable
    private val service = GroupRecordUploadService(uploads, links, storage, references, deletions, scheduler, transactions)

    @BeforeEach
    fun `발급 트랜잭션과 일회성 스케줄러를 준비한다`() {
        TransactionSynchronizationManager.initSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(true)
        given(scheduler.schedule(any<Runnable>(), any<Instant>())).willAnswer {
            task = it.getArgument(0)
            future
        }
        given(transactions.getTransaction(any())).willAnswer { SimpleTransactionStatus() }
    }

    @AfterEach
    fun `트랜잭션 상태를 해제한다`() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization()
        TransactionSynchronizationManager.setActualTransactionActive(false)
    }

    @Test
    fun `발급 트랜잭션이 커밋되면 저장된 확인 시각의 일회성 예약을 유지한다`() {
        val file = upload()

        service.scheduleUploadCheck(file.id, file.uploadCheckAt)
        TransactionSynchronizationManager.getSynchronizations().forEach {
            it.afterCompletion(TransactionSynchronization.STATUS_COMMITTED)
        }

        verify(scheduler).schedule(any<Runnable>(), eq(file.uploadCheckAt))
        verifyNoInteractions(future, uploads, links, storage, references, deletions)
    }

    @Test
    fun `발급 트랜잭션이 롤백되면 일회성 예약을 취소한다`() {
        service.scheduleUploadCheck("file", Instant.now().plusSeconds(660))
        TransactionSynchronizationManager.getSynchronizations().forEach {
            it.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        }

        verify(future).cancel(false)
        verifyNoInteractions(uploads, links, storage, references, deletions)
    }

    @Test
    fun `예약 등록 실패를 발급 요청으로 전달한다`() {
        given(scheduler.schedule(any<Runnable>(), any<Instant>())).willThrow(RejectedExecutionException())

        assertThrows<RejectedExecutionException> { service.scheduleUploadCheck("file", Instant.now()) }

        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty())
        verifyNoInteractions(uploads, links, storage, references, deletions)
    }

    @Test
    fun `발급 트랜잭션 없이 확인을 예약하지 않는다`() {
        TransactionSynchronizationManager.setActualTransactionActive(false)

        assertThrows<IllegalStateException> { service.scheduleUploadCheck("file", Instant.now()) }

        verifyNoInteractions(scheduler)
    }

    @Test
    fun `예약 시각에 객체가 있으면 완료 알림 없이도 업로드 완료로 표시한다`() {
        val file = upload()
        given(storage.isFileExists(file.fileKey)).willReturn(true)

        runCheck(file)

        assertTrue(file.isUploaded)
        verifyNoInteractions(links, references, deletions)
        verify(transactions).commit(any())
    }

    @Test
    fun `객체가 없으면 기록과 임시저장 부가 행을 먼저 지우고 공통 참조를 해제한다`() {
        val file = upload()
        val record = view(file, FileReferenceType.GROUP_RECORD_ATTACHMENT, 3L)
        val draft = view(file, FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, 4L)
        given(links.findByFileUploadIdOrderById(file.id)).willReturn(listOf(record, draft))

        runCheck(file)

        inOrder(links, references) {
            verify(links).deleteAll(listOf(record.link, draft.link))
            verify(links).flush()
            verify(references).deleteFileReference(record.reference.id, record.reference.referenceType, "3")
            verify(references).deleteFileReference(draft.reference.id, draft.reference.referenceType, "4")
        }
        verifyNoInteractions(deletions)
        verify(transactions).commit(any())
    }

    @Test
    fun `객체와 기록 연결이 모두 없으면 기존 파일 삭제 흐름에 맡긴다`() {
        val file = upload()
        given(storage.isFileExists(file.fileKey)).willThrow(StorageOperationException(StorageFailureReason.NO_SUCH_KEY, "객체가 없습니다."))
        given(links.findByFileUploadIdOrderById(file.id)).willReturn(emptyList())

        runCheck(file)

        verify(deletions).deleteFileUpload(file.id)
        verifyNoInteractions(references)
    }

    @Test
    fun `R2 확인에 실패하면 미업로드로 간주하지 않고 연결을 유지한다`() {
        val file = upload()
        given(storage.isFileExists(file.fileKey)).willThrow(StorageOperationException(StorageFailureReason.TRANSIENT, "일시적인 오류입니다."))

        runCheck(file)

        assertFalse(file.isUploaded)
        verifyNoInteractions(links, references, deletions)
        verify(transactions).rollback(any())
    }

    @Test
    fun `완료되었거나 삭제되었거나 확인 시각 전인 파일은 처리하지 않는다`() {
        val files =
            listOf(
                upload().apply { markUploaded() },
                upload().apply { markDeleted(Instant.now()) },
                upload(createdAt = Instant.now()),
            )

        files.forEach { runCheck(it) }

        verifyNoInteractions(storage, links, references, deletions)
    }

    @Test
    fun `이미 제거된 업로드의 예약은 아무것도 변경하지 않는다`() {
        service.scheduleUploadCheck("missing", Instant.EPOCH)

        task.run()

        verifyNoInteractions(storage, links, references, deletions)
    }

    private fun runCheck(file: FileUpload) {
        given(uploads.findForUpdateById(file.id)).willReturn(file)
        service.scheduleUploadCheck(file.id, file.uploadCheckAt)
        task.run()
    }

    private fun upload(createdAt: Instant = Instant.EPOCH) =
        FileUpload("file", "key", 2L, "/groups/1/records/files", "file.txt", 1L, "text/plain", createdAt)

    private fun view(
        file: FileUpload,
        type: FileReferenceType,
        ownerId: Long,
    ): GroupRecordFileView {
        val reference = FileReference("ref-$ownerId", file.id, type, ownerId.toString())
        return GroupRecordFileView(GroupRecordFile(ownerId, reference.id, "block-$ownerId"), reference, file)
    }
}
