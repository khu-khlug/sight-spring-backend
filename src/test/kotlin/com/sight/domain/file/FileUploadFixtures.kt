package com.sight.domain.file

import java.time.Instant

fun testFileUpload(
    id: String = "file-1",
    fileKey: String = "key-1",
    uploaderMemberId: Long = 10L,
    apiPath: String = "/groups/1/activity-report/upload-link",
    isUploaded: Boolean = false,
    deletedAt: Instant? = null,
    createdAt: Instant = Instant.parse("2025-01-01T00:00:00Z"),
): FileUpload =
    FileUpload(
        id = id,
        fileKey = fileKey,
        uploaderMemberId = uploaderMemberId,
        apiPath = apiPath,
        fileName = null,
        size = null,
        contentType = null,
        createdAt = createdAt,
    ).apply {
        if (isUploaded) markUploaded()
        if (deletedAt != null) markDeleted(deletedAt)
    }
