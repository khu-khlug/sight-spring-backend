package com.sight.service

import com.sight.domain.file.FileReferenceType
import com.sight.domain.group.GroupFileType
import com.sight.repository.FileReferenceRepository
import com.sight.repository.GroupFileRepository
import org.springframework.stereotype.Service

@Service
class GroupFileDownloadService(
    private val groupFiles: GroupFileRepository,
    private val references: FileReferenceRepository,
    private val downloads: FileDownloadService,
) {
    fun getGroupFileUrl(groupId: Long, fileId: String): String =
        downloads.getDownloadUrl(fileId) { uploadId -> isGroupFileAllowed(groupId, uploadId) }

    private fun isGroupFileAllowed(groupId: Long, fileId: String): Boolean {
        if (!groupFiles.existsByGroupIdAndFileUploadId(groupId, fileId)) return false
        val types = resolveGroupFileTypes(fileId)
        return types.isNotEmpty() && ALLOWED_TYPES.containsAll(types)
    }

    private fun resolveGroupFileTypes(fileId: String): Set<GroupFileType> =
        references.findDistinctTypesByFileUploadId(fileId).mapTo(mutableSetOf()) { type ->
            when (type) {
                FileReferenceType.GROUP_RECORD_ATTACHMENT -> GroupFileType.RECORD_ATTACHMENT
                FileReferenceType.GROUP_RECORD_DRAFT_ATTACHMENT -> GroupFileType.RECORD_DRAFT_ATTACHMENT
                FileReferenceType.GROUP_CARD_COVER -> GroupFileType.CARD_COVER
                FileReferenceType.GROUP_ACTIVITY_REPORT -> GroupFileType.ACTIVITY_REPORT
            }
        }

    private companion object {
        val ALLOWED_TYPES = setOf(GroupFileType.RECORD_ATTACHMENT, GroupFileType.RECORD_DRAFT_ATTACHMENT, GroupFileType.CARD_COVER)
    }
}
