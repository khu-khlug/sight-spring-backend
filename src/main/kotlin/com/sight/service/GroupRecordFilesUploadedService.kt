package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.repository.FileUploadRepository
import com.sight.repository.GroupFileRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RecordFileVerificationResult(val fileId: String, val isSuccess: Boolean, val reason: String?)

@Service
class GroupRecordFilesUploadedService(
    private val groups: GroupRepository,
    private val uploads: FileUploadRepository,
    private val groupFiles: GroupFileRepository,
    private val storage: StorageService,
    private val members: MemberRepository,
) {
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
        val lockedUploads = fileIds.distinct().sorted().associateWith { uploads.findForUpdateById(it) }
        return fileIds.distinct().map { fileId ->
            val upload = lockedUploads[fileId]
            val reason =
                when {
                    upload == null -> "NOT_FOUND"
                    upload.apiPath != "/groups/$groupId/records/files" ||
                        !groupFiles.existsByGroupIdAndFileUploadId(groupId, fileId) -> "NOT_FOUND"
                    !upload.isIssuedBy(memberId) -> "FORBIDDEN"
                    upload.isDeleted -> "DELETED"
                    !storage.isFileExists(upload.fileKey) -> "NOT_UPLOADED"
                    else -> null
                }
            if (reason == null) upload!!.markUploaded()
            RecordFileVerificationResult(fileId, reason == null, reason)
        }
    }
}
