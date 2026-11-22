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
    private val links = mock<GroupRecordFileRepository>()
    private val uploads = mock<FileUploadRepository>()
    private val groupFiles = mock<GroupFileRepository>()
    private val ids = mock<IntegerIdService>()
    private val storage = mock<StorageService>()
    private val references = mock<FileReferenceService>()
    private val groups = mock<GroupRepository>()
    private val members = mock<MemberRepository>()
    private val uploadChecks = mock<GroupRecordUploadService>()
    private val service =
        GroupRecordAttachmentService(links, uploads, groupFiles, ids, storage, references, groups, members, uploadChecks)

    @Test
    fun `최대 크기의 새 첨부를 같은 크기로 서명하고 발급한 파일을 응답과 참조에 연결한다`() {
        val size = MAX_FILE_BYTES
        given(ids.nextId()).willReturn(50L, 51L)
        given(storage.generateUploadUrl(any(), any())).willReturn("https://upload.example.com")
        given(links.findByTypeAndOwnerOrderById(RECORD, "3")).willReturn(emptyList())
        given(references.createFileReference(any(), any(), any())).willAnswer {
            FileReference("ref", it.getArgument(0), it.getArgument(1), it.getArgument(2))
        }

        val result = service.replaceRecordFiles(1L, 2L, 3L, listOf(newFile(size)))

        val saved = argumentCaptor<FileUpload>()
        verify(uploads).saveAndFlush(saved.capture())
        val file = saved.firstValue
        assertEquals(size, file.size)
        assertEquals("/groups/1/records/files", file.apiPath)
        verify(storage).generateUploadUrl(file.fileKey, size)
        verify(uploadChecks).scheduleUploadCheck(file.id, file.uploadCheckAt)
        verify(references).createFileReference(file.id, RECORD, "3")
        val link = argumentCaptor<GroupRecordFile>()
        verify(links).save(link.capture())
        assertEquals("ref", link.firstValue.fileReferenceId)
        assertEquals("block", link.firstValue.fileBlockId)
        val expected = RecordFileUploadResult("block", file.id, "/groups/1/files/${file.id}", "https://upload.example.com")
        assertEquals(expected, result.uploads.single())
        assertTrue(result.isChanged)
    }

    @Test
    fun `크기 범위를 벗어나거나 블록 아이디가 중복되면 업로드를 발급하지 않는다`() {
        val invalid = listOf(listOf(newFile(-1)), listOf(newFile(MAX_FILE_BYTES + 1)), List(2) { newFile(1) })
        invalid.forEach { files ->
            assertThrows<BadRequestException> { service.replaceRecordFiles(1L, 2L, 3L, files) }
        }
        verifyNoInteractions(links, uploads, groupFiles, storage, references, uploadChecks)
    }

    @Test
    fun `다른 그룹에서 발급된 파일과 같은 블록의 파일 교체를 거부한다`() {
        val original = view("old", RECORD, "3", "block")
        val foreign = upload("foreign", apiPath = "/groups/9/records/files")
        given(links.findByTypeAndOwnerOrderById(RECORD, "3")).willReturn(listOf(original))
        given(uploads.findForUpdateById("old")).willReturn(original.upload)
        given(uploads.findForUpdateById("foreign")).willReturn(foreign)

        assertThrows<BadRequestException> {
            service.replaceRecordFiles(1L, 2L, 3L, listOf(RecordFileInput("block", "foreign", null, null, null)))
        }
        assertThrows<BadRequestException> {
            service.replaceRecordFiles(1L, 2L, 3L, listOf(RecordFileInput("new-block", "foreign", null, null, null)))
        }
        verifyNoInteractions(references, storage)
    }

    @Test
    fun `같은 파일의 블록을 교체할 때 새 참조를 저장한 뒤 이전 참조를 해제한다`() {
        val old = view("shared", RECORD, "3", "old-block")
        given(links.findByTypeAndOwnerOrderById(RECORD, "3")).willReturn(listOf(old))
        given(uploads.findForUpdateById("shared")).willReturn(old.upload)
        given(groupFiles.existsByGroupIdAndFileUploadId(1L, "shared")).willReturn(true)
        given(ids.nextId()).willReturn(51L)
        given(references.createFileReference("shared", RECORD, "3")).willReturn(FileReference("new-ref", "shared", RECORD, "3"))

        val result = service.replaceRecordFiles(1L, 2L, 3L, listOf(RecordFileInput("new-block", "shared", null, null, null)))

        assertEquals(RecordFilesChangeResult(emptyList(), true), result)
        inOrder(links, references) {
            verify(references).createFileReference("shared", RECORD, "3")
            verify(links).save(any())
            verify(links).flush()
            verify(links).deleteAll(listOf(old.link))
            verify(links).flush()
            verify(references).deleteFileReference(old.reference.id, RECORD, "3")
        }
        verifyNoInteractions(storage)
    }

    @Test
    fun `업로드 완료 확인은 잠금 순서와 응답 순서를 구분하고 파일마다 실패 사유를 반환한다`() {
        given(members.findForUpdateById(2L)).willReturn(mock())
        given(groups.findLockedById(1L)).willReturn(testGroup())
        val success = upload("z-ok")
        val pending = upload("a-pending")
        val deleted = upload("b-deleted").apply { markDeleted(NOW) }
        val forbidden = upload("c-forbidden", memberId = 9L)
        val foreign = upload("d-foreign", apiPath = "/groups/9/records/files")
        listOf(success, pending, deleted, forbidden, foreign).forEach {
            given(uploads.findForUpdateById(it.id)).willReturn(it)
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
        inOrder(uploads) {
            requested.distinct().sorted().forEach { verify(uploads).findForUpdateById(it) }
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

    private fun upload(
        id: String,
        apiPath: String = "/groups/1/records/files",
        memberId: Long = 2L,
    ) = FileUpload(id, "key-$id", memberId, apiPath, "file.txt", 1L, "text/plain", NOW)

    private fun view(
        fileId: String,
        type: FileReferenceType,
        owner: String,
        block: String,
    ): GroupRecordFileView {
        val reference = FileReference("ref-$owner", fileId, type, owner)
        return GroupRecordFileView(GroupRecordFile(owner.toLong(), reference.id, block), reference, upload(fileId))
    }

    companion object {
        private const val MAX_FILE_BYTES = 20L * 1024 * 1024
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
        private val RECORD = FileReferenceType.GROUP_RECORD_ATTACHMENT
    }
}
