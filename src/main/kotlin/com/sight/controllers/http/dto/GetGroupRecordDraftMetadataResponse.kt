package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class GetGroupRecordDraftMetadataResponse(val draft: GroupRecordDraftMetadataResponse?)

data class GroupRecordDraftMetadataResponse(
    @get:JsonProperty("isNewRecord") val isNewRecord: Boolean,
    val cardId: String,
    val cardTitle: String,
    val recordId: String?,
    val updatedAt: Instant,
)
