package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class GetGroupRecordDraftResponse(val draft: GroupRecordDraftResponse)

data class GroupRecordDraftResponse(
    @get:JsonProperty("isNewRecord") val isNewRecord: Boolean,
    val cardId: String,
    val cardTitle: String,
    val recordId: String?,
    val content: String,
    val updatedAt: Instant,
    val files: List<GroupRecordDraftFileResponse>,
)

data class GroupRecordDraftFileResponse(
    val id: String,
    val fileId: String,
    val url: String?,
    val name: String,
    val size: Long,
    val contentType: String,
)
