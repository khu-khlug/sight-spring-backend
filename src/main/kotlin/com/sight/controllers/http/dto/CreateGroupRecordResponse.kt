package com.sight.controllers.http.dto

import java.time.Instant

data class CreateGroupRecordResponse(
    val id: String,
    val type: String,
    val authorName: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isDeleted: Boolean,
    val isInPortfolio: Boolean,
    val isInUserArchive: Boolean,
    val files: List<CreatedRecordFileResponse>,
    val uploads: List<RecordFileUploadResponse>,
)

data class CreatedRecordFileResponse(
    val id: String,
    val fileId: String,
    val url: String?,
    val name: String,
    val size: Long,
    val contentType: String,
)
