package com.sight.service

import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.testFileUpload
import com.sight.repository.FileReferenceRepository
import com.sight.repository.FileUploadRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.Instant
import java.util.Optional

class FileUploadMaintenanceServiceTest {
    private val uploads = mock<FileUploadRepository>()
    private val references = mock<FileReferenceRepository>()
    private val storage = mock<StorageService>()
    private val deletions = mock<FileUploadDeletionService>()
    private val registry = mock<FileCleanupRegistry>()
    private val transactions = mock<PlatformTransactionManager>()
    private val predicate = mock<(FileCleanupContext) -> Boolean>()
    private val release = mock<(FileReference) -> Unit>()
    private val file = testFileUpload(createdAt = Instant.EPOCH)
    private val reference = FileReference("ref", file.id, FileReferenceType.GROUP_RECORD_ATTACHMENT, "3")
    private val at = Instant.parse("2026-11-22T00:00:00Z")

    @BeforeEach
    fun `파일과 참조를 준비한다`() {
        given(transactions.getTransaction(any())).willAnswer { SimpleTransactionStatus() }
        given(uploads.findById(file.id)).willReturn(Optional.of(file))
        given(uploads.findForUpdateById(file.id)).willReturn(file)
        given(uploads.markUnreferencedForDeletion(file.id, at)).willReturn(1)
        given(references.findByFileUploadIdOrderById(eq(file.id), any(), any())).willReturn(listOf(reference))
        given(registry.bindings).willReturn(listOf(FileCleanupBinding(reference.referenceType, predicate, release)))
        given(deletions.deleteMarkedFileUploadForMaintenance(file.id)).willReturn(FileUploadDeletionResult.DELETED)
    }

    @Test
    fun `정리 대상으로 판단한 참조는 부가 행과 공통 참조를 순서대로 해제한 뒤 파일을 삭제한다`() {
        given(references.existsByFileUploadId(file.id)).willReturn(true, false)
        given(predicate.invoke(any())).willReturn(true)

        val result = service().cleanupUpload(file.id, at)

        assertEquals(FileUploadDeletionResult.DELETED, result)
        inOrder(predicate, release, references, uploads, deletions) {
            verify(predicate).invoke(any())
            verify(release).invoke(reference)
            verify(references).deleteByIdAndRefTypeAndReferrer(reference.id, reference.referenceType, reference.referrerId)
            verify(uploads).markUnreferencedForDeletion(file.id, at)
            verify(deletions).deleteMarkedFileUploadForMaintenance(file.id)
        }
        assertTrue(file.isDeleted)
    }

    @Test
    fun `사용 중인 참조가 하나라도 남으면 파일을 유지한다`() {
        given(references.existsByFileUploadId(file.id)).willReturn(true)
        given(storage.isFileExists(file.fileKey)).willReturn(true)
        given(predicate.invoke(any())).willReturn(false)

        assertEquals(FileUploadDeletionResult.SKIPPED, service().cleanupUpload(file.id, at))

        verifyNoInteractions(release, deletions)
        verify(references, never()).deleteByIdAndRefTypeAndReferrer(any(), any(), any())
        assertFalse(file.isDeleted)
    }

    @Test
    fun `판단 함수를 등록하지 않은 참조 종류는 유지한다`() {
        given(registry.bindings).willReturn(emptyList())
        given(references.existsByFileUploadId(file.id)).willReturn(true)

        assertEquals(FileUploadDeletionResult.SKIPPED, service().cleanupUpload(file.id, at))

        verifyNoInteractions(predicate, release, deletions)
        verify(references, never()).deleteByIdAndRefTypeAndReferrer(any(), any(), any())
    }

    @Test
    fun `R2 확인 실패는 연결 삭제로 이어지지 않는다`() {
        given(references.existsByFileUploadId(file.id)).willReturn(true)
        given(storage.isFileExists(file.fileKey)).willThrow(StorageOperationException(StorageFailureReason.AUTHENTICATION, "인증 실패입니다."))

        assertThrows<StorageOperationException> { service().cleanupUpload(file.id, at) }

        verifyNoInteractions(predicate, release, deletions)
    }

    @Test
    fun `확인 중 완료 알림이 들어오면 최신 업로드 상태로 판단한다`() {
        given(references.existsByFileUploadId(file.id)).willReturn(true)
        given(storage.isFileExists(file.fileKey)).willAnswer {
            file.markUploaded()
            false
        }

        service().cleanupUpload(file.id, at)

        verify(predicate).invoke(org.mockito.kotlin.argThat { upload.isUploaded })
        verifyNoInteractions(release, deletions)
    }

    @Test
    fun `참조 없는 파일의 삭제도 조건부 갱신에 성공해야 진행한다`() {
        given(references.findByFileUploadIdOrderById(eq(file.id), any(), any())).willReturn(emptyList())
        given(uploads.markUnreferencedForDeletion(file.id, at)).willReturn(0)

        assertEquals(FileUploadDeletionResult.SKIPPED, service().cleanupUpload(file.id, at))

        verifyNoInteractions(storage, predicate, release, deletions)
    }

    @Test
    fun `부가 행 해제에 실패하면 파일을 삭제하지 않고 트랜잭션을 롤백한다`() {
        given(references.existsByFileUploadId(file.id)).willReturn(true)
        given(predicate.invoke(any())).willReturn(true)
        given(release.invoke(reference)).willThrow(IllegalStateException("연결 해제 실패입니다."))

        assertThrows<IllegalStateException> { service().cleanupUpload(file.id, at) }

        verify(references, never()).deleteByIdAndRefTypeAndReferrer(any(), any(), any())
        verify(transactions).rollback(any())
        verifyNoInteractions(deletions)
    }

    private fun service() = FileUploadMaintenanceService(uploads, references, storage, deletions, registry, transactions)
}
