package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupFile
import com.sight.domain.group.GroupRecordFile
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import com.sight.repository.GroupRecordFileRepository
import com.sight.repository.dto.GroupRecordFileView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GroupRecordAttachmentService(
    private val links: GroupRecordFileRepository,
    private val uploads: FileUploadRepository,
    private val groupFiles: GroupFileRepository,
    private val integerIds: IntegerIdService,
    private val storage: StorageService,
    private val references: FileReferenceService,
) {
    @Transactional
    fun replaceRecordFiles(
        groupId: Long,
        memberId: Long,
        recordId: Long,
        files: List<RecordFileInput>,
    ): RecordFilesChangeResult = replace(groupId, memberId, FileReferenceType.GROUP_RECORD_ATTACHMENT, recordId.toString(), files, false)

    @Transactional
    fun replaceDraftFiles(
        groupId: Long,
        memberId: Long,
        draftId: Long,
        files: List<RecordFileInput>,
        reset: Boolean = false,
    ): RecordFilesChangeResult =
        replace(groupId, memberId, FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, draftId.toString(), files, reset)

    @Transactional
    fun deleteDraftFiles(draftId: Long) {
        val existing = links.findByTypeAndOwnerOrderById(FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, draftId.toString())
        lockUploads(existing.map { it.reference.fileUploadId })
        deleteLinks(existing)
    }

    @Transactional(readOnly = true)
    fun getDraftFiles(
        groupId: Long,
        draftId: Long,
    ): List<RecordFileResult> =
        readFiles(groupId, links.findByTypeAndOwnerOrderById(FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT, draftId.toString()))

    @Transactional(readOnly = true)
    fun getRecordFiles(
        groupId: Long,
        recordId: Long,
    ): List<RecordFileResult> =
        readFiles(groupId, links.findByTypeAndOwnerOrderById(FileReferenceType.GROUP_RECORD_ATTACHMENT, recordId.toString()))

    private fun readFiles(
        groupId: Long,
        existing: List<GroupRecordFileView>,
    ): List<RecordFileResult> =
        existing.mapNotNull { view ->
            view.upload.takeUnless { it.isDeleted }?.let { file ->
                RecordFileResult(
                    view.link.fileBlockId,
                    file.id,
                    if (file.isUploaded) fileUrl(groupId, file.id) else null,
                    requireNotNull(file.fileName),
                    requireNotNull(file.size),
                    requireNotNull(file.contentType),
                )
            }
        }

    private fun replace(
        groupId: Long,
        memberId: Long,
        type: FileReferenceType,
        ownerId: String,
        files: List<RecordFileInput>,
        reset: Boolean,
    ): RecordFilesChangeResult {
        validateFiles(files)
        val existing = links.findByTypeAndOwnerOrderById(type, ownerId)
        val oldByBlockId = if (reset) emptyMap() else existing.associateBy { it.link.fileBlockId }
        val locked = lockUploads(existing.map { it.reference.fileUploadId } + files.mapNotNull { it.fileId })
        files.forEach { input ->
            val old = oldByBlockId[input.id]
            if (old != null && old.reference.fileUploadId != input.fileId) throw BadRequestException("연결된 파일 블록의 파일은 바꿀 수 없습니다.")
            input.fileId?.let { id ->
                val file = locked[id] ?: throw BadRequestException("파일을 찾을 수 없습니다.")
                if (file.isDeleted || file.apiPath != "/groups/$groupId/records/files" ||
                    file.fileName == null || file.size == null || file.contentType == null ||
                    !groupFiles.existsByGroupIdAndFileUploadId(groupId, id)
                ) {
                    throw BadRequestException("이 그룹의 유효한 기록 첨부 파일이 아닙니다.")
                }
            }
        }
        val blockIds = files.map { it.id }.toSet()
        val removed = existing.filter { reset || it.link.fileBlockId !in blockIds }
        val result = mutableListOf<RecordFileUploadResult>()
        files.filter { it.id !in oldByBlockId }.forEach { input ->
            val fileId = input.fileId ?: createUpload(groupId, memberId, input, result)
            val reference = references.createFileReference(fileId, type, ownerId)
            links.save(GroupRecordFile(integerIds.nextId(), reference.id, input.id))
        }
        links.flush()
        // 새 참조를 먼저 저장해 같은 파일의 연결 교체 중에는 마지막 참조가 사라지지 않도록 한다.
        deleteLinks(removed)
        return RecordFilesChangeResult(result, removed.isNotEmpty() || files.any { it.id !in oldByBlockId })
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
        uploads.saveAndFlush(
            FileUpload(
                fileId,
                fileKey,
                memberId,
                "/groups/$groupId/records/files",
                input.name,
                size,
                input.contentType,
                Instant.now(),
            ),
        )
        groupFiles.save(GroupFile(integerIds.nextId(), groupId, fileId))
        result += RecordFileUploadResult(input.id, fileId, fileUrl(groupId, fileId), uploadUrl)
        return fileId
    }

    private fun lockUploads(ids: List<String>): Map<String, FileUpload> =
        ids.distinct().sorted().mapNotNull { uploads.findForUpdateById(it) }.associateBy { it.id }

    private fun deleteLinks(existing: List<GroupRecordFileView>) {
        links.deleteAll(existing.map { it.link })
        links.flush()
        existing.forEach { view ->
            references.deleteFileReference(view.reference.id, view.reference.referenceType, view.reference.ownerId)
        }
    }

    private fun fileUrl(
        groupId: Long,
        fileId: String,
    ): String = "/groups/$groupId/files/$fileId"
}
