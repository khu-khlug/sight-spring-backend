package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupRecordFile
import com.sight.domain.group.testGroup
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import com.sight.repository.GroupRecordFileRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import com.sight.repository.dto.GroupRecordFileView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant

class GroupRecordAttachmentServiceTest {
    private val recordFileRepository = mock<GroupRecordFileRepository>()
    private val fileUploadRepository = mock<FileUploadRepository>()
    private val groupFiles = mock<GroupFileRepository>()
    private val ids = mock<IntegerIdService>()
    private val storage = mock<StorageService>()
    private val fileReferenceService = mock<FileReferenceService>()
    private val groups = mock<GroupRepository>()
    private val members = mock<MemberRepository>()
    private val uploadChecks = mock<GroupRecordUploadService>()
    private val service =
        GroupRecordAttachmentService(
            recordFileRepository, fileUploadRepository, groupFiles, ids, storage, fileReferenceService, groups, members, uploadChecks,
        )

    @Test
    fun `최대 크기의 새 첨부를 같은 크기로 서명하고 발급한 파일을 응답과 참조에 연결한다`() {
        val size = MAX_FILE_BYTES
        given(ids.nextId()).willReturn(50L, 51L)
        given(storage.generateUploadUrl(any(), any())).willReturn("https://upload.example.com")
        given(recordFileRepository.findByRefTypeAndReferrerOrderById(RECORD, "3")).willReturn(emptyList())
        given(fileReferenceService.createFileReference(any(), any(), any())).willAnswer {
            FileReference("ref", it.getArgument(0), it.getArgument(1), it.getArgument(2))
        }

        val result = service.replaceRecordFiles(1L, 2L, 3L, listOf(newFile(size)))

        val saved = argumentCaptor<FileUpload>()
        verify(fileUploadRepository).saveAndFlush(saved.capture())
        val fileUpload = saved.firstValue
        assertEquals(size, fileUpload.size)
        assertEquals("/groups/1/records/files", fileUpload.apiPath)
        verify(storage).generateUploadUrl(fileUpload.fileKey, size)
        verify(uploadChecks).scheduleUploadCheck(fileUpload.id, fileUpload.uploadCheckAt)
        verify(fileReferenceService).createFileReference(fileUpload.id, RECORD, "3")
        val recordFile = argumentCaptor<GroupRecordFile>()
        verify(recordFileRepository).save(recordFile.capture())
        assertEquals("ref", recordFile.firstValue.fileReferenceId)
        assertEquals("block", recordFile.firstValue.fileBlockId)
        val expected = RecordFileUploadResult("block", fileUpload.id, "/groups/1/files/${fileUpload.id}", "https://upload.example.com")
        assertEquals(expected, result.uploads.single())
        assertTrue(result.isChanged)
    }

    @Test
    fun `크기 범위를 벗어나거나 블록 아이디가 중복되면 업로드를 발급하지 않는다`() {
        val invalid = listOf(listOf(newFile(-1)), listOf(newFile(MAX_FILE_BYTES + 1)), List(2) { newFile(1) })
        invalid.forEach { files ->
            assertThrows<BadRequestException> { service.replaceRecordFiles(1L, 2L, 3L, files) }
        }
        verifyNoInteractions(recordFileRepository, fileUploadRepository, groupFiles, storage, fileReferenceService, uploadChecks)
    }

    @Test
    fun `다른 그룹에서 발급된 파일과 같은 블록의 파일 교체를 거부한다`() {
        val original = view("old", RECORD, "3", "block")
        val foreign = fileUpload("foreign", apiPath = GroupFilePaths.recordAttachmentApiPath(9L))
        given(recordFileRepository.findByRefTypeAndReferrerOrderById(RECORD, "3")).willReturn(listOf(original))
        given(fileUploadRepository.findForUpdateById("old")).willReturn(original.fileUpload)
        given(fileUploadRepository.findForUpdateById("foreign")).willReturn(foreign)

        assertThrows<BadRequestException> {
            service.replaceRecordFiles(1L, 2L, 3L, listOf(RecordFileInput("block", "foreign", null, null, null)))
        }
        assertThrows<BadRequestException> {
            service.replaceRecordFiles(1L, 2L, 3L, listOf(RecordFileInput("new-block", "foreign", null, null, null)))
        }
        verifyNoInteractions(fileReferenceService, storage)
    }

    @Test
    fun `같은 파일의 블록을 교체할 때 새 참조를 저장한 뒤 이전 참조를 해제한다`() {
        val old = view("shared", RECORD, "3", "old-block")
        given(recordFileRepository.findByRefTypeAndReferrerOrderById(RECORD, "3")).willReturn(listOf(old))
        given(fileUploadRepository.findForUpdateById("shared")).willReturn(old.fileUpload)
        given(groupFiles.existsByGroupIdAndFileUploadId(1L, "shared")).willReturn(true)
        given(ids.nextId()).willReturn(51L)
        given(fileReferenceService.createFileReference("shared", RECORD, "3")).willReturn(FileReference("new-ref", "shared", RECORD, "3"))

        val result = service.replaceRecordFiles(1L, 2L, 3L, listOf(RecordFileInput("new-block", "shared", null, null, null)))

        assertEquals(RecordFilesChangeResult(emptyList(), true), result)
        inOrder(recordFileRepository, fileReferenceService) {
            verify(fileReferenceService).createFileReference("shared", RECORD, "3")
            verify(recordFileRepository).save(any())
            verify(recordFileRepository).flush()
            verify(recordFileRepository).deleteAll(listOf(old.recordFile))
            verify(recordFileRepository).flush()
            verify(fileReferenceService).deleteFileReference(old.fileReference.id, RECORD, "3")
        }
        verifyNoInteractions(storage)
    }

    @Test
    fun `업로드 완료 확인은 잠금 순서와 응답 순서를 구분하고 파일마다 실패 사유를 반환한다`() {
        given(members.findForUpdateById(2L)).willReturn(mock())
        given(groups.findLockedById(1L)).willReturn(testGroup())
        val success = fileUpload("z-ok")
        val pending = fileUpload("a-pending")
        val deleted = fileUpload("b-deleted").apply { markDeleted(NOW) }
        val forbidden = fileUpload("c-forbidden", memberId = 9L)
        val foreign = fileUpload("d-foreign", apiPath = GroupFilePaths.recordAttachmentApiPath(9L))
        listOf(success, pending, deleted, forbidden, foreign).forEach {
            given(fileUploadRepository.findForUpdateById(it.id)).willReturn(it)
            given(groupFiles.existsByGroupIdAndFileUploadId(1L, it.id)).willReturn(true)
        }
        given(storage.isFileExists(success.fileKey)).willReturn(true)
        val requested = listOf(success.id, pending.id, success.id, deleted.id, forbidden.id, foreign.id, "e-missing")

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, requested)

        assertEquals(
            listOf(
                RecordFileVerificationResult(success.id, true, null),
                RecordFileVerificationResult(pending.id, false, "NOT_UPLOADED"),
                RecordFileVerificationResult(deleted.id, false, "DELETED"),
                RecordFileVerificationResult(forbidden.id, false, "FORBIDDEN"),
                RecordFileVerificationResult(foreign.id, false, "NOT_FOUND"),
                RecordFileVerificationResult("e-missing", false, "NOT_FOUND"),
            ),
            result,
        )
        inOrder(fileUploadRepository) {
            requested.distinct().sorted().forEach { verify(fileUploadRepository).findForUpdateById(it) }
        }
        verify(storage).isFileExists(success.fileKey)
        verify(storage).isFileExists(pending.fileKey)
        assertTrue(success.isUploaded)
        assertFalse(pending.isUploaded)
        assertFalse(forbidden.isUploaded)
        assertFalse(foreign.isUploaded)

        given(storage.isFileExists(pending.fileKey)).willReturn(true)
        assertTrue(service.updateGroupRecordFilesUploaded(1L, 2L, listOf(pending.id)).single().isSuccess)
        assertTrue(pending.isUploaded)
        assertTrue(service.updateGroupRecordFilesUploaded(1L, 2L, listOf(pending.id)).single().isSuccess)
    }

    private fun newFile(size: Long) = RecordFileInput("block", null, "file.txt", size, "text/plain")

    private fun fileUpload(
        id: String,
        apiPath: String = GroupFilePaths.recordAttachmentApiPath(1L),
        memberId: Long = 2L,
    ) = FileUpload(id, "key-$id", memberId, apiPath, "file.txt", 1L, "text/plain", NOW)

    private fun view(
        fileId: String,
        type: FileReferenceType,
        referrerId: String,
        block: String,
    ): GroupRecordFileView {
        val fileReference = FileReference("ref-$referrerId", fileId, type, referrerId)
        return GroupRecordFileView(GroupRecordFile(referrerId.toLong(), fileReference.id, block), fileReference, fileUpload(fileId))
    }

    companion object {
        private const val MAX_FILE_BYTES = 20L * 1024 * 1024
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
        private val RECORD = FileReferenceType.GROUP_RECORD_ATTACHMENT
    }
}
