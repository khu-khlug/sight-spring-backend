package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class CreateGroupRecordResponse(
    val id: String,
    val type: String,
    val authorName: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    @get:JsonProperty("isDeleted") val isDeleted: Boolean,
    @get:JsonProperty("isInPortfolio") val isInPortfolio: Boolean,
    @get:JsonProperty("isInUserArchive") val isInUserArchive: Boolean,
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
