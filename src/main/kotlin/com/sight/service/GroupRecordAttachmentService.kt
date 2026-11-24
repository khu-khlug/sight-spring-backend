package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupFile
import com.sight.domain.group.GroupRecordFile
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import com.sight.repository.GroupRecordFileRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import com.sight.repository.dto.GroupRecordFileView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class RecordFileVerificationResult(val fileId: String, val isSuccess: Boolean, val reason: String?)

@Service
class GroupRecordAttachmentService(
    private val recordFileRepository: GroupRecordFileRepository,
    private val fileUploadRepository: FileUploadRepository,
    private val groupFiles: GroupFileRepository,
    private val integerIds: IntegerIdService,
    private val storage: StorageService,
    private val fileReferenceService: FileReferenceService,
    private val groups: GroupRepository,
    private val members: MemberRepository,
    private val uploadChecks: GroupRecordUploadService,
) {
    @Transactional
    fun replaceRecordFiles(
        groupId: Long,
        memberId: Long,
        recordId: Long,
        files: List<RecordFileInput>,
    ): List<RecordFileUploadResult> =
        replace(
            groupId,
            memberId,
            FileReferenceType.GROUP_RECORD_ATTACHMENT,
            recordId.toString(),
            files,
            false,
        )

    @Transactional
    fun replaceDraftFiles(
        groupId: Long,
        memberId: Long,
        draftId: Long,
        files: List<RecordFileInput>,
        reset: Boolean = false,
    ): List<RecordFileUploadResult> =
        replace(groupId, memberId, FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, draftId.toString(), files, reset)

    @Transactional
    fun deleteDraftFiles(draftId: Long) {
        val existingAttachments =
            recordFileRepository.findByRefTypeAndReferrerOrderById(FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, draftId.toString())
        lockFileUploads(existingAttachments.map { it.fileReference.fileUploadId })
        deleteAttachmentReferences(existingAttachments)
    }

    @Transactional(readOnly = true)
    fun getDraftFiles(
        groupId: Long,
        draftId: Long,
    ): List<RecordFileResult> =
        readFiles(
            groupId,
            recordFileRepository.findByRefTypeAndReferrerOrderById(FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, draftId.toString()),
        )

    @Transactional(readOnly = true)
    fun getRecordFiles(
        groupId: Long,
        recordId: Long,
    ): List<RecordFileResult> =
        readFiles(
            groupId,
            recordFileRepository.findByRefTypeAndReferrerOrderById(FileReferenceType.GROUP_RECORD_ATTACHMENT, recordId.toString()),
        )

    private fun readFiles(
        groupId: Long,
        existingAttachments: List<GroupRecordFileView>,
    ): List<RecordFileResult> =
        existingAttachments.mapNotNull { attachment ->
            attachment.fileUpload.takeUnless { it.isDeleted }?.let { fileUpload ->
                RecordFileResult(
                    attachment.recordFile.fileBlockId,
                    fileUpload.id,
                    if (fileUpload.isUploaded) GroupFilePaths.fileUrl(groupId, fileUpload.id) else null,
                    requireNotNull(fileUpload.fileName),
                    requireNotNull(fileUpload.size),
                    requireNotNull(fileUpload.contentType),
                )
            }
        }

    private fun replace(
        groupId: Long,
        memberId: Long,
        refType: FileReferenceType,
        referrerId: String,
        files: List<RecordFileInput>,
        reset: Boolean,
    ): List<RecordFileUploadResult> {
        validateFiles(files)
        val existingAttachments = recordFileRepository.findByRefTypeAndReferrerOrderById(refType, referrerId)
        val attachmentsByBlockId = if (reset) emptyMap() else existingAttachments.associateBy { it.recordFile.fileBlockId }
        val lockedFileUploads = lockFileUploads(existingAttachments.map { it.fileReference.fileUploadId } + files.mapNotNull { it.fileId })
        files.forEach { input ->
            val existingAttachment = attachmentsByBlockId[input.id]
            if (existingAttachment != null && existingAttachment.fileReference.fileUploadId != input.fileId) {
                throw BadRequestException("연결된 파일 블록의 파일은 바꿀 수 없습니다.")
            }
            input.fileId?.let { id ->
                val fileUpload = lockedFileUploads[id] ?: throw BadRequestException("파일을 찾을 수 없습니다.")
                if (fileUpload.isDeleted || fileUpload.apiPath != GroupFilePaths.recordAttachmentApiPath(groupId) ||
                    fileUpload.fileName == null || fileUpload.size == null || fileUpload.contentType == null ||
                    !groupFiles.existsByGroupIdAndFileUploadId(groupId, id)
                ) {
                    throw BadRequestException("이 그룹의 유효한 기록 첨부 파일이 아닙니다.")
                }
            }
        }
        val blockIds = files.map { it.id }.toSet()
        val removedAttachments = existingAttachments.filter { reset || it.recordFile.fileBlockId !in blockIds }
        val result = mutableListOf<RecordFileUploadResult>()
        files.filter { it.id !in attachmentsByBlockId }.forEach { input ->
            val fileId = input.fileId ?: createUpload(groupId, memberId, input, result)
            val fileReference = fileReferenceService.createFileReference(fileId, refType, referrerId)
            recordFileRepository.save(GroupRecordFile(integerIds.nextId(), fileReference.id, input.id))
        }
        recordFileRepository.flush()
        // 새 참조를 먼저 저장해 같은 파일의 연결 교체 중에는 마지막 참조가 사라지지 않도록 한다.
        deleteAttachmentReferences(removedAttachments)
        return result
    }

    private fun validateFiles(files: List<RecordFileInput>) {
        if (files.map { it.id }.toSet().size != files.size) throw BadRequestException("파일 블록 아이디가 중복되었습니다.")
        files.forEach { input ->
            if (input.id.isBlank() || input.id.length > GroupRecordFile.FILE_BLOCK_ID_MAX_LENGTH) {
                throw BadRequestException(
                    "파일 블록 아이디가 올바르지 않습니다.",
                )
            }
            if (input.fileId == null) {
                if (input.name.isNullOrBlank() || input.name.length > 255 ||
                    input.contentType.isNullOrBlank() || input.contentType.length > 255 ||
                    input.size == null || input.size !in 0..FileUpload.MAX_FILE_SIZE
                ) {
                    throw BadRequestException("새 파일의 이름·크기·MIME 타입이 올바르지 않습니다.")
                }
            } else if (input.fileId.isBlank() || input.fileId.length > FileUpload.ID_MAX_LENGTH) {
                throw BadRequestException("파일 아이디가 올바르지 않습니다.")
            }
        }
    }

    private fun createUpload(
        groupId: Long,
        memberId: Long,
        input: RecordFileInput,
        result: MutableList<RecordFileUploadResult>,
    ): String {
        val fileId = UlidCreator.getUlid().toString()
        val fileKey = UlidCreator.getUlid().toString()
        val size = requireNotNull(input.size)
        val uploadUrl = storage.generateUploadUrl(fileKey, size)
        val fileUpload =
            FileUpload(
                fileId,
                fileKey,
                memberId,
                GroupFilePaths.recordAttachmentApiPath(groupId),
                input.name,
                size,
                input.contentType,
                Instant.now(),
            )
        fileUploadRepository.saveAndFlush(fileUpload)
        uploadChecks.scheduleUploadCheck(fileUpload.id, fileUpload.uploadCheckAt)
        groupFiles.save(GroupFile(integerIds.nextId(), groupId, fileId))
        result += RecordFileUploadResult(input.id, fileId, GroupFilePaths.fileUrl(groupId, fileId), uploadUrl)
        return fileId
    }

    private fun lockFileUploads(ids: List<String>): Map<String, FileUpload> =
        ids.distinct().sorted().mapNotNull { fileUploadRepository.findForUpdateById(it) }.associateBy { it.id }

    private fun deleteAttachmentReferences(existingAttachments: List<GroupRecordFileView>) {
        recordFileRepository.deleteAll(existingAttachments.map { it.recordFile })
        recordFileRepository.flush()
        existingAttachments.forEach { attachment ->
            fileReferenceService.deleteFileReference(
                attachment.fileReference.id,
                attachment.fileReference.referenceType,
                attachment.fileReference.referrerId,
            )
        }
    }

    @Transactional
    fun updateGroupRecordFilesUploaded(
        groupId: Long,
        memberId: Long,
        fileIds: List<String>,
    ): List<RecordFileVerificationResult> {
        members.findForUpdateById(memberId) ?: throw NotFoundException("회원을 찾을 수 없습니다.")
        val group = groups.findLockedById(groupId) ?: throw NotFoundException("그룹을 찾을 수 없습니다.")
        if (group.status.isEnded) throw BadRequestException("종료된 그룹입니다.")
        if (fileIds.isEmpty()) throw BadRequestException("업로드한 파일 목록이 필요합니다.")
        val lockedUploads = fileIds.distinct().sorted().associateWith { fileUploadRepository.findForUpdateById(it) }
        return fileIds.distinct().map { fileId ->
            val fileUpload = lockedUploads[fileId]
            val reason =
                when {
                    fileUpload == null -> "NOT_FOUND"
                    fileUpload.apiPath != GroupFilePaths.recordAttachmentApiPath(groupId) ||
                        !groupFiles.existsByGroupIdAndFileUploadId(groupId, fileId) -> "NOT_FOUND"
                    !fileUpload.isIssuedBy(memberId) -> "FORBIDDEN"
                    fileUpload.isDeleted -> "DELETED"
                    !storage.isFileExists(fileUpload.fileKey) -> "NOT_UPLOADED"
                    else -> null
                }
            if (reason == null) fileUpload!!.markUploaded()
            RecordFileVerificationResult(fileId, reason == null, reason)
        }
    }
}
