package com.sight.service

internal object GroupFilePaths {
    fun recordAttachmentApiPath(groupId: Long): String = "/groups/$groupId/records/files"

    fun fileUrl(
        groupId: Long,
        fileId: String,
    ): String = "/groups/$groupId/files/$fileId"
}
