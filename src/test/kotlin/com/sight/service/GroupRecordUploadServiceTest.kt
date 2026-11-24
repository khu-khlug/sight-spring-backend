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
        GroupRecordUploadService(
            fileUploadRepository,
            recordFileRepository,
            storage,
            fileReferenceService,
            deletions,
            scheduler,
            transactions,
        )

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
    fun `업로드 주소 발급 트랜잭션이 커밋되면 저장된 확인 예정 시각의 예약을 취소하지 않는다`() {
        val fileUpload = fileUpload()

        service.scheduleUploadCheck(fileUpload.id, fileUpload.uploadCheckAt)
        TransactionSynchronizationManager.getSynchronizations().forEach {
            it.afterCompletion(TransactionSynchronization.STATUS_COMMITTED)
        }

        verify(scheduler).schedule(any<Runnable>(), eq(fileUpload.uploadCheckAt))
        verifyNoInteractions(future, fileUploadRepository, recordFileRepository, storage, fileReferenceService, deletions)
    }

    @Test
    fun `업로드 주소 발급 트랜잭션이 롤백되면 업로드 확인 예약을 취소한다`() {
        service.scheduleUploadCheck("file", Instant.now().plusSeconds(660))
        TransactionSynchronizationManager.getSynchronizations().forEach {
            it.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        }

        verify(future).cancel(false)
        verifyNoInteractions(fileUploadRepository, recordFileRepository, storage, fileReferenceService, deletions)
    }

    @Test
    fun `스케줄러가 업로드 확인 예약을 거부하면 예외를 호출자에게 전달한다`() {
        given(scheduler.schedule(any<Runnable>(), any<Instant>())).willThrow(RejectedExecutionException())

        assertThrows<RejectedExecutionException> { service.scheduleUploadCheck("file", Instant.now()) }

        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty())
        verifyNoInteractions(fileUploadRepository, recordFileRepository, storage, fileReferenceService, deletions)
    }

    @Test
    fun `활성 트랜잭션이 없으면 업로드 확인을 예약할 수 없다`() {
        TransactionSynchronizationManager.setActualTransactionActive(false)

        assertThrows<IllegalStateException> { service.scheduleUploadCheck("file", Instant.now()) }

        verifyNoInteractions(scheduler)
    }

    @Test
    fun `확인 예정 시각이 지난 파일이 R2에 있으면 업로드 완료로 표시한다`() {
        val fileUpload = fileUpload()
        given(storage.isFileExists(fileUpload.fileKey)).willReturn(true)

        runCheck(fileUpload)

        assertTrue(fileUpload.isUploaded)
        verifyNoInteractions(recordFileRepository, fileReferenceService, deletions)
        verify(transactions).commit(any())
    }

    @Test
    fun `R2에 파일이 없으면 기록과 임시저장의 첨부 연결을 삭제한 뒤 공통 파일 참조를 해제한다`() {
        val fileUpload = fileUpload()
        val recordAttachment = view(fileUpload, FileReferenceType.GROUP_RECORD_ATTACHMENT, 3L)
        val draftAttachment = view(fileUpload, FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, 4L)
        given(recordFileRepository.findByFileUploadIdOrderById(fileUpload.id)).willReturn(listOf(recordAttachment, draftAttachment))

        runCheck(fileUpload)

        inOrder(recordFileRepository, fileReferenceService) {
            verify(recordFileRepository).deleteAll(listOf(recordAttachment.recordFile, draftAttachment.recordFile))
            verify(recordFileRepository).flush()
            verify(
                fileReferenceService,
            ).deleteFileReference(recordAttachment.fileReference.id, recordAttachment.fileReference.referenceType, "3")
            verify(
                fileReferenceService,
            ).deleteFileReference(draftAttachment.fileReference.id, draftAttachment.fileReference.referenceType, "4")
        }
        verifyNoInteractions(deletions)
        verify(transactions).commit(any())
    }

    @Test
    fun `R2에 파일이 없고 첨부 연결도 없으면 파일 삭제를 요청한다`() {
        val fileUpload = fileUpload()
        given(storage.isFileExists(fileUpload.fileKey)).willThrow(StorageOperationException(StorageFailureReason.NO_SUCH_KEY, "객체가 없습니다."))
        given(recordFileRepository.findByFileUploadIdOrderById(fileUpload.id)).willReturn(emptyList())

        runCheck(fileUpload)

        verify(deletions).deleteFileUpload(fileUpload.id)
        verifyNoInteractions(fileReferenceService)
    }

    @Test
    fun `R2 조회 중 일시적인 오류가 발생하면 파일 참조를 유지하고 트랜잭션을 롤백한다`() {
        val fileUpload = fileUpload()
        given(storage.isFileExists(fileUpload.fileKey)).willThrow(StorageOperationException(StorageFailureReason.TRANSIENT, "일시적인 오류입니다."))

        runCheck(fileUpload)

        assertFalse(fileUpload.isUploaded)
        verifyNoInteractions(recordFileRepository, fileReferenceService, deletions)
        verify(transactions).rollback(any())
    }

    @Test
    fun `업로드가 완료되었거나 삭제 표시가 있거나 확인 예정 시각이 지나지 않은 파일은 R2에서 조회하지 않는다`() {
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
    fun `예약 실행 시 업로드 기록이 없으면 파일과 참조를 처리하지 않는다`() {
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
