package com.sight.service

import com.sight.core.config.ConfigKey
import com.sight.core.config.SystemConfigRegistry
import com.sight.core.exception.BadRequestException
import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupRecordFile
import com.sight.domain.group.testGroup
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import com.sight.repository.GroupRecordDraftRepository
import com.sight.repository.GroupRecordFileRepository
import com.sight.repository.GroupRecordRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import com.sight.repository.dto.GroupRecordFileView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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
    private val systemConfigRegistry = mock<SystemConfigRegistry>()
    private val records = mock<GroupRecordRepository>()
    private val drafts = mock<GroupRecordDraftRepository>()
    private val service =
        GroupRecordAttachmentService(
            recordFileRepository,
            fileUploadRepository,
            groupFiles,
            ids,
            storage,
            fileReferenceService,
            groups,
            members,
            uploadChecks,
            FileUploadSizePolicyService(systemConfigRegistry),
            records,
            drafts,
        )

    @BeforeEach
    fun setUp() {
        given(systemConfigRegistry.getValueAsLong(any())).willAnswer { it.getArgument<ConfigKey>(0).defaultValue.toLong() }
    }

    @Test
    fun `기록 첨부 한도를 넘으면 기본 20 MiB보다 작아도 기록과 임시저장 모두 업로드 주소를 발급하지 않는다`() {
        val maxSizeBytes = 5L * 1024 * 1024
        given(systemConfigRegistry.getValueAsLong(ConfigKey.GROUP_RECORD_FILE_MAX_SIZE_BYTES)).willReturn(maxSizeBytes)
        val files = listOf(newFile(maxSizeBytes + 1))

        assertThrows<BadRequestException> { service.replaceRecordFiles(1L, 2L, 3L, files) }
        assertThrows<BadRequestException> { service.replaceDraftFiles(1L, 2L, 3L, files) }

        verifyNoInteractions(recordFileRepository, fileUploadRepository, groupFiles, storage, fileReferenceService, uploadChecks)
    }

    @Test
    fun `20 MiB 파일을 첨부하면 파일 크기로 서명한 업로드 주소를 발급하고 파일 참조를 생성한다`() {
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
        assertEquals(expected, result.single())
    }

    @Test
    fun `첨부 파일의 크기가 음수이면 업로드 주소를 발급하지 않는다`() {
        assertThrows<BadRequestException> { service.replaceRecordFiles(1L, 2L, 3L, listOf(newFile(-1))) }

        verifyNoInteractions(recordFileRepository, fileUploadRepository, groupFiles, storage, fileReferenceService, uploadChecks)
    }

    @Test
    fun `첨부 파일의 크기가 20 MiB를 넘으면 업로드 주소를 발급하지 않는다`() {
        assertThrows<BadRequestException> { service.replaceRecordFiles(1L, 2L, 3L, listOf(newFile(MAX_FILE_BYTES + 1))) }

        verifyNoInteractions(recordFileRepository, fileUploadRepository, groupFiles, storage, fileReferenceService, uploadChecks)
    }

    @Test
    fun `첨부 파일의 블록 아이디가 중복되면 업로드 주소를 발급하지 않는다`() {
        val files = List(2) { newFile(1) }

        assertThrows<BadRequestException> { service.replaceRecordFiles(1L, 2L, 3L, files) }

        verifyNoInteractions(recordFileRepository, fileUploadRepository, groupFiles, storage, fileReferenceService, uploadChecks)
    }

    @Test
    fun `다른 그룹의 파일은 기록에 첨부할 수 없다`() {
        val foreign = fileUpload("foreign", apiPath = GroupFilePaths.recordAttachmentApiPath(9L))
        given(recordFileRepository.findByRefTypeAndReferrerOrderById(RECORD, "3")).willReturn(emptyList())
        given(fileUploadRepository.findForUpdateById("foreign")).willReturn(foreign)

        assertThrows<BadRequestException> {
            service.replaceRecordFiles(1L, 2L, 3L, listOf(RecordFileInput("new-block", "foreign", null, null, null)))
        }
        verifyNoInteractions(fileReferenceService, storage)
    }

    @Test
    fun `이미 연결된 첨부 블록의 파일은 다른 파일로 바꿀 수 없다`() {
        val original = view("old", RECORD, "3", "block")
        val replacement = fileUpload("replacement")
        given(recordFileRepository.findByRefTypeAndReferrerOrderById(RECORD, "3")).willReturn(listOf(original))
        given(fileUploadRepository.findForUpdateById("old")).willReturn(original.fileUpload)
        given(fileUploadRepository.findForUpdateById("replacement")).willReturn(replacement)
        given(groupFiles.existsByGroupIdAndFileUploadId(1L, "replacement")).willReturn(true)

        assertThrows<BadRequestException> {
            service.replaceRecordFiles(1L, 2L, 3L, listOf(RecordFileInput("block", "replacement", null, null, null)))
        }
        verifyNoInteractions(fileReferenceService, storage)
    }

    @Test
    fun `같은 파일을 참조하는 새 블록 추가와 기존 블록 삭제를 함께 요청할 때 새 참조를 먼저 저장한다`() {
        val old = view("shared", RECORD, "3", "old-block")
        given(recordFileRepository.findByRefTypeAndReferrerOrderById(RECORD, "3")).willReturn(listOf(old))
        given(fileUploadRepository.findForUpdateById("shared")).willReturn(old.fileUpload)
        given(groupFiles.existsByGroupIdAndFileUploadId(1L, "shared")).willReturn(true)
        given(ids.nextId()).willReturn(51L)
        given(fileReferenceService.createFileReference("shared", RECORD, "3")).willReturn(FileReference("new-ref", "shared", RECORD, "3"))

        val result = service.replaceRecordFiles(1L, 2L, 3L, listOf(RecordFileInput("new-block", "shared", null, null, null)))

        assertTrue(result.isEmpty())
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
    fun `업로드 완료를 확인할 때 요청 순서와 관계없이 파일 아이디 순으로 잠근다`() {
        val firstFile = fileUpload("a-file")
        val secondFile = fileUpload("b-file")
        val thirdFile = fileUpload("c-file")
        prepareUploadVerification(firstFile, secondFile, thirdFile)

        service.updateGroupRecordFilesUploaded(1L, 2L, listOf(thirdFile.id, firstFile.id, secondFile.id))

        inOrder(fileUploadRepository) {
            verify(fileUploadRepository).findForUpdateById(firstFile.id)
            verify(fileUploadRepository).findForUpdateById(secondFile.id)
            verify(fileUploadRepository).findForUpdateById(thirdFile.id)
        }
    }

    @Test
    fun `업로드 완료 확인 결과는 성공 여부와 관계없이 요청 순서대로 반환한다`() {
        val success = fileUpload("z-ok")
        val pending = fileUpload("a-pending")
        prepareUploadVerification(success, pending)
        given(storage.isFileExists(pending.fileKey)).willReturn(false)

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(success.id, pending.id))

        assertEquals(
            listOf(
                RecordFileVerificationResult(success.id, true, null),
                RecordFileVerificationResult(pending.id, false, "NOT_UPLOADED"),
            ),
            result,
        )
    }

    @Test
    fun `업로드 완료 확인 요청에 같은 파일 아이디가 여러 번 있어도 한 번만 처리하고 결과도 하나만 반환한다`() {
        val fileUpload = fileUpload("repeated")
        prepareUploadVerification(fileUpload)

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(fileUpload.id, fileUpload.id))

        assertEquals(listOf(RecordFileVerificationResult(fileUpload.id, true, null)), result)
        verify(fileUploadRepository).findForUpdateById(fileUpload.id)
        verify(storage).isFileExists(fileUpload.fileKey)
    }

    @Test
    fun `R2에 파일이 있으면 업로드 완료로 표시하고 성공 결과를 반환한다`() {
        val fileUpload = fileUpload("uploaded")
        prepareUploadVerification(fileUpload)

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(fileUpload.id))

        assertEquals(RecordFileVerificationResult(fileUpload.id, true, null), result.single())
        assertTrue(fileUpload.isUploaded)
    }

    @Test
    fun `업로드 기록이 없는 파일은 NOT_FOUND를 반환한다`() {
        prepareUploadVerification()

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, listOf("missing"))

        assertEquals(RecordFileVerificationResult("missing", false, "NOT_FOUND"), result.single())
        verifyNoInteractions(storage)
    }

    @Test
    fun `다른 그룹의 첨부 파일은 업로드 완료 확인 시 NOT_FOUND를 반환한다`() {
        val fileUpload = fileUpload("foreign", apiPath = GroupFilePaths.recordAttachmentApiPath(9L))
        prepareUploadVerification(fileUpload)

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(fileUpload.id))

        assertEquals(RecordFileVerificationResult(fileUpload.id, false, "NOT_FOUND"), result.single())
        assertFalse(fileUpload.isUploaded)
        verifyNoInteractions(storage)
    }

    @Test
    fun `다른 회원이 업로드 링크를 발급받은 파일은 FORBIDDEN을 반환한다`() {
        val fileUpload = fileUpload("forbidden", memberId = 9L)
        prepareUploadVerification(fileUpload)

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(fileUpload.id))

        assertEquals(RecordFileVerificationResult(fileUpload.id, false, "FORBIDDEN"), result.single())
        assertFalse(fileUpload.isUploaded)
        verifyNoInteractions(storage)
    }

    @Test
    fun `삭제 표시된 파일은 업로드 완료 확인 시 DELETED를 반환한다`() {
        val fileUpload = fileUpload("deleted").apply { markDeleted(NOW) }
        prepareUploadVerification(fileUpload)

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(fileUpload.id))

        assertEquals(RecordFileVerificationResult(fileUpload.id, false, "DELETED"), result.single())
        assertFalse(fileUpload.isUploaded)
        verifyNoInteractions(storage)
    }

    @Test
    fun `R2에 파일이 없으면 완료로 표시하지 않고 NOT_UPLOADED를 반환한다`() {
        val fileUpload = fileUpload("pending")
        prepareUploadVerification(fileUpload)
        given(storage.isFileExists(fileUpload.fileKey)).willReturn(false)

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(fileUpload.id))

        assertEquals(RecordFileVerificationResult(fileUpload.id, false, "NOT_UPLOADED"), result.single())
        assertFalse(fileUpload.isUploaded)
    }

    @Test
    fun `R2에 파일이 없어 완료 확인에 실패해도 파일을 업로드한 뒤 다시 요청하면 성공한다`() {
        val fileUpload = fileUpload("retry")
        prepareUploadVerification(fileUpload)
        given(storage.isFileExists(fileUpload.fileKey)).willReturn(false, true)

        val firstResult = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(fileUpload.id))
        val retryResult = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(fileUpload.id))

        assertEquals(RecordFileVerificationResult(fileUpload.id, false, "NOT_UPLOADED"), firstResult.single())
        assertEquals(RecordFileVerificationResult(fileUpload.id, true, null), retryResult.single())
        assertTrue(fileUpload.isUploaded)
    }

    @Test
    fun `이미 업로드 완료로 표시된 파일도 완료 확인을 다시 요청하면 성공한다`() {
        val fileUpload = fileUpload("already-uploaded").apply { markUploaded() }
        prepareUploadVerification(fileUpload)

        val result = service.updateGroupRecordFilesUploaded(1L, 2L, listOf(fileUpload.id))

        assertEquals(RecordFileVerificationResult(fileUpload.id, true, null), result.single())
        assertTrue(fileUpload.isUploaded)
    }

    private fun prepareUploadVerification(vararg fileUploads: FileUpload) {
        given(members.findForUpdateById(2L)).willReturn(mock())
        given(groups.findLockedById(1L)).willReturn(testGroup())
        fileUploads.forEach { fileUpload ->
            given(fileUploadRepository.findForUpdateById(fileUpload.id)).willReturn(fileUpload)
            given(groupFiles.existsByGroupIdAndFileUploadId(1L, fileUpload.id)).willReturn(true)
            given(storage.isFileExists(fileUpload.fileKey)).willReturn(true)
        }
    }

    @Test
    fun `업로드가 미완료이고 R2에 객체가 없으면 첨부 참조를 정리 대상으로 판단한다`() {
        val view = view("missing", RECORD, "3", "block")

        assertTrue(service.isAttachmentCleanupTarget(FileCleanupContext(view.fileUpload, view.fileReference, false)))
    }

    @Test
    fun `원본 기록이 남아 있으면 복구 가능한 기록의 첨부를 유지한다`() {
        val view = view("record", RECORD, "3", "block")
        given(records.existsIncludingDeletedById(3L)).willReturn(true)

        assertFalse(service.isAttachmentCleanupTarget(FileCleanupContext(view.fileUpload, view.fileReference, true)))
    }

    @Test
    fun `임시저장이 남아 있으면 임시저장의 첨부를 유지한다`() {
        val view = view("draft", FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, "4", "block")
        given(drafts.existsById(4L)).willReturn(true)

        assertFalse(service.isAttachmentCleanupTarget(FileCleanupContext(view.fileUpload, view.fileReference, true)))
    }

    @Test
    fun `원본 기록이 실제로 없어지면 업로드가 끝난 첨부 참조도 정리 대상으로 판단한다`() {
        val view = view("orphan", RECORD, "3", "block")
        view.fileUpload.markUploaded()

        assertTrue(service.isAttachmentCleanupTarget(FileCleanupContext(view.fileUpload, view.fileReference, null)))
    }

    @Test
    fun `주기적 청소에서 첨부 부가 행을 제거하면 공통 참조를 해제하기 전에 반영한다`() {
        val view = view("orphan", RECORD, "3", "block")
        given(recordFileRepository.findByFileReferenceId(view.fileReference.id)).willReturn(view.recordFile)

        service.deleteAttachmentDetailsForCleanup(view.fileReference)

        inOrder(recordFileRepository) {
            verify(recordFileRepository).delete(view.recordFile)
            verify(recordFileRepository).flush()
        }
        verifyNoInteractions(fileReferenceService, fileUploadRepository, storage)
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
