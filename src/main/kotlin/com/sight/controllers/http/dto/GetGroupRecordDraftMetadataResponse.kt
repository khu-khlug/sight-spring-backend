package com.sight.controllers.http.dto

import java.time.Instant

data class GetGroupRecordDraftMetadataResponse(val draft: GroupRecordDraftMetadataResponse?)

data class GroupRecordDraftMetadataResponse(
    val isNewRecord: Boolean,
    val cardId: String,
    val cardTitle: String,
    val recordId: String?,
    val updatedAt: Instant,
)
