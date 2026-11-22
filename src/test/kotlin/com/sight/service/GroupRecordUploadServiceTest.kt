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
    private val fileUploadRepository = mock<FileUploadRepository>()
    private val recordFileRepository = mock<GroupRecordFileRepository>()
    private val storage = mock<StorageService>()
    private val fileReferenceService = mock<FileReferenceService>()
    private val deletions = mock<FileUploadDeletionService>()
    private val scheduler = mock<TaskScheduler>()
    private val transactions = mock<PlatformTransactionManager>()
    private val future = mock<ScheduledFuture<*>>()
    private lateinit var task: Runnable
    private val service =
        GroupRecordUploadService(fileUploadRepository, recordFileRepository, storage, fileReferenceService, deletions, scheduler, transactions)

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
        val fileUpload = fileUpload()

        service.scheduleUploadCheck(fileUpload.id, fileUpload.uploadCheckAt)
        TransactionSynchronizationManager.getSynchronizations().forEach {
            it.afterCompletion(TransactionSynchronization.STATUS_COMMITTED)
        }

        verify(scheduler).schedule(any<Runnable>(), eq(fileUpload.uploadCheckAt))
        verifyNoInteractions(future, fileUploadRepository, recordFileRepository, storage, fileReferenceService, deletions)
    }

    @Test
    fun `발급 트랜잭션이 롤백되면 일회성 예약을 취소한다`() {
        service.scheduleUploadCheck("file", Instant.now().plusSeconds(660))
        TransactionSynchronizationManager.getSynchronizations().forEach {
            it.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        }

        verify(future).cancel(false)
        verifyNoInteractions(fileUploadRepository, recordFileRepository, storage, fileReferenceService, deletions)
    }

    @Test
    fun `예약 등록 실패를 발급 요청으로 전달한다`() {
        given(scheduler.schedule(any<Runnable>(), any<Instant>())).willThrow(RejectedExecutionException())

        assertThrows<RejectedExecutionException> { service.scheduleUploadCheck("file", Instant.now()) }

        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty())
        verifyNoInteractions(fileUploadRepository, recordFileRepository, storage, fileReferenceService, deletions)
    }

    @Test
    fun `발급 트랜잭션 없이 확인을 예약하지 않는다`() {
        TransactionSynchronizationManager.setActualTransactionActive(false)

        assertThrows<IllegalStateException> { service.scheduleUploadCheck("file", Instant.now()) }

        verifyNoInteractions(scheduler)
    }

    @Test
    fun `예약 시각에 객체가 있으면 완료 알림 없이도 업로드 완료로 표시한다`() {
        val fileUpload = fileUpload()
        given(storage.isFileExists(fileUpload.fileKey)).willReturn(true)

        runCheck(fileUpload)

        assertTrue(fileUpload.isUploaded)
        verifyNoInteractions(recordFileRepository, fileReferenceService, deletions)
        verify(transactions).commit(any())
    }

    @Test
    fun `객체가 없으면 기록과 임시저장 부가 행을 먼저 지우고 공통 참조를 해제한다`() {
        val fileUpload = fileUpload()
        val recordAttachment = view(fileUpload, FileReferenceType.GROUP_RECORD_ATTACHMENT, 3L)
        val draftAttachment = view(fileUpload, FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, 4L)
        given(recordFileRepository.findByFileUploadIdOrderById(fileUpload.id)).willReturn(listOf(recordAttachment, draftAttachment))

        runCheck(fileUpload)

        inOrder(recordFileRepository, fileReferenceService) {
            verify(recordFileRepository).deleteAll(listOf(recordAttachment.recordFile, draftAttachment.recordFile))
            verify(recordFileRepository).flush()
            verify(fileReferenceService).deleteFileReference(recordAttachment.fileReference.id, recordAttachment.fileReference.referenceType, "3")
            verify(fileReferenceService).deleteFileReference(draftAttachment.fileReference.id, draftAttachment.fileReference.referenceType, "4")
        }
        verifyNoInteractions(deletions)
        verify(transactions).commit(any())
    }

    @Test
    fun `객체와 기록 연결이 모두 없으면 기존 파일 삭제 흐름에 맡긴다`() {
        val fileUpload = fileUpload()
        given(storage.isFileExists(fileUpload.fileKey)).willThrow(StorageOperationException(StorageFailureReason.NO_SUCH_KEY, "객체가 없습니다."))
        given(recordFileRepository.findByFileUploadIdOrderById(fileUpload.id)).willReturn(emptyList())

        runCheck(fileUpload)

        verify(deletions).deleteFileUpload(fileUpload.id)
        verifyNoInteractions(fileReferenceService)
    }

    @Test
    fun `R2 확인에 실패하면 미업로드로 간주하지 않고 연결을 유지한다`() {
        val fileUpload = fileUpload()
        given(storage.isFileExists(fileUpload.fileKey)).willThrow(StorageOperationException(StorageFailureReason.TRANSIENT, "일시적인 오류입니다."))

        runCheck(fileUpload)

        assertFalse(fileUpload.isUploaded)
        verifyNoInteractions(recordFileRepository, fileReferenceService, deletions)
        verify(transactions).rollback(any())
    }

    @Test
    fun `완료되었거나 삭제되었거나 확인 시각 전인 파일은 처리하지 않는다`() {
        val files =
            listOf(
                fileUpload().apply { markUploaded() },
                fileUpload().apply { markDeleted(Instant.now()) },
                fileUpload(createdAt = Instant.now()),
            )

        files.forEach { runCheck(it) }

        verifyNoInteractions(storage, recordFileRepository, fileReferenceService, deletions)
    }

    @Test
    fun `이미 제거된 업로드의 예약은 아무것도 변경하지 않는다`() {
        service.scheduleUploadCheck("missing", Instant.EPOCH)

        task.run()

        verifyNoInteractions(storage, recordFileRepository, fileReferenceService, deletions)
    }

    private fun runCheck(fileUpload: FileUpload) {
        given(fileUploadRepository.findForUpdateById(fileUpload.id)).willReturn(fileUpload)
        service.scheduleUploadCheck(fileUpload.id, fileUpload.uploadCheckAt)
        task.run()
    }

    private fun fileUpload(createdAt: Instant = Instant.EPOCH) =
        FileUpload("file", "key", 2L, GroupFilePaths.recordAttachmentApiPath(1L), "file.txt", 1L, "text/plain", createdAt)

    private fun view(
        fileUpload: FileUpload,
        type: FileReferenceType,
        referrerId: Long,
    ): GroupRecordFileView {
        val fileReference = FileReference("ref-$referrerId", fileUpload.id, type, referrerId.toString())
        return GroupRecordFileView(GroupRecordFile(referrerId, fileReference.id, "block-$referrerId"), fileReference, fileUpload)
    }
}
