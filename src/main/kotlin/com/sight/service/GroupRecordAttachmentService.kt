package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupFile
import com.sight.domain.group.GroupRecordFile
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import com.sight.repository.GroupRecordFileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant

@Service
class GroupRecordAttachmentService(
    private val links: GroupRecordFileRepository,
    private val uploads: FileUploadRepository,
    private val groupFiles: GroupFileRepository,
    private val integerIds: IntegerIdService,
    private val storage: StorageService,
    private val lifecycle: GroupRecordFileLifecycleService,
) {
    @Transactional
    fun replaceRecordFiles(
        groupId: Long,
        memberId: Long,
        recordId: Long,
        files: List<RecordFileInput>,
    ): RecordFilesChangeResult =
        replace(groupId, memberId, links.findByRecordIdOrderByIdAsc(recordId), files, false) { identifier, fileId ->
            GroupRecordFile.ofRecord(integerIds.nextId(), recordId, identifier, fileId)
        }

    @Transactional
    fun replaceDraftFiles(
        groupId: Long,
        memberId: Long,
        draftId: Long,
        files: List<RecordFileInput>,
        reset: Boolean = false,
    ): RecordFilesChangeResult =
        replace(groupId, memberId, links.findByDraftIdOrderByIdAsc(draftId), files, reset) { identifier, fileId ->
            GroupRecordFile.ofDraft(integerIds.nextId(), draftId, identifier, fileId)
        }

    @Transactional
    fun deleteDraftFiles(draftId: Long) {
        val existing = links.findByDraftIdOrderByIdAsc(draftId)
        val locked = lockUploads(existing.map { it.fileUploadId })
        links.deleteAll(existing)
        links.flush()
        removeOrphans(locked.values)
    }

    @Transactional(readOnly = true)
    fun getDraftFiles(
        groupId: Long,
        draftId: Long,
    ): List<RecordFileResult> = readFiles(groupId, links.findByDraftIdOrderByIdAsc(draftId))

    @Transactional(readOnly = true)
    fun getRecordFiles(
        groupId: Long,
        recordId: Long,
    ): List<RecordFileResult> = readFiles(groupId, links.findByRecordIdOrderByIdAsc(recordId))

    private fun readFiles(
        groupId: Long,
        existing: List<GroupRecordFile>,
    ): List<RecordFileResult> {
        val files = uploads.findAllById(existing.map { it.fileUploadId }).associateBy { it.id }
        return existing.mapNotNull { link ->
            files[link.fileUploadId]?.takeUnless { it.isDeleted }?.let { file ->
                RecordFileResult(
                    link.fileIdentifier,
                    file.id,
                    if (file.isUploaded) fileUrl(groupId, file.id) else null,
                    requireNotNull(file.fileName),
                    requireNotNull(file.size),
                    requireNotNull(file.contentType),
                )
            }
        }
    }

    private fun replace(
        groupId: Long,
        memberId: Long,
        existing: List<GroupRecordFile>,
        files: List<RecordFileInput>,
        reset: Boolean,
        newLink: (String, String) -> GroupRecordFile,
    ): RecordFilesChangeResult {
        validateFiles(files)
        val oldByIdentifier = if (reset) emptyMap() else existing.associateBy { it.fileIdentifier }
        val locked = lockUploads(existing.map { it.fileUploadId } + files.mapNotNull { it.fileId })
        files.forEach { input ->
            val old = oldByIdentifier[input.id]
            if (old != null && old.fileUploadId != input.fileId) throw BadRequestException("연결된 식별자의 파일은 바꿀 수 없습니다.")
            input.fileId?.let { id ->
                val file = locked[id] ?: throw BadRequestException("파일을 찾을 수 없습니다.")
                if (file.isDeleted || !groupFiles.existsByGroupIdAndFileUploadId(groupId, id)) {
                    throw BadRequestException("이 그룹의 유효한 파일이 아닙니다.")
                }
            }
        }
        val identifiers = files.map { it.id }.toSet()
        val removed = existing.filter { reset || it.fileIdentifier !in identifiers }
        links.deleteAll(removed)
        links.flush()
        val result = mutableListOf<RecordFileUploadResult>()
        files.filter { it.id !in oldByIdentifier }.forEach { input ->
            val fileId = input.fileId ?: createUpload(groupId, memberId, input, result)
            links.save(newLink(input.id, fileId))
        }
        links.flush()
        removeOrphans(removed.mapNotNull { locked[it.fileUploadId] }.distinctBy { it.id })
        return RecordFilesChangeResult(result, removed.isNotEmpty() || files.any { it.id !in oldByIdentifier })
    }

    private fun validateFiles(files: List<RecordFileInput>) {
        if (files.map { it.id }.toSet().size != files.size) throw BadRequestException("파일 식별자가 중복되었습니다.")
        files.forEach { input ->
            if (input.id.isBlank() || input.id.length > GroupRecordFile.FILE_IDENTIFIER_MAX_LENGTH) {
                throw BadRequestException(
                    "파일 식별자가 올바르지 않습니다.",
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
        val uploadUrl = storage.generateUploadUrl(fileKey)
        uploads.save(
            FileUpload(
                fileId,
                fileKey,
                memberId,
                "/groups/$groupId/records/files",
                input.name,
                input.size,
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

    private fun removeOrphans(files: Collection<FileUpload>) {
        files.filter { !it.isDeleted && !links.existsByFileUploadId(it.id) }.forEach { file ->
            file.markDeleted(Instant.now())
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        lifecycle.deleteMarkedUpload(file.id)
                    }
                },
            )
        }
    }

    private fun fileUrl(
        groupId: Long,
        fileId: String,
    ): String = "/groups/$groupId/files/$fileId"
}
