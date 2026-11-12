package com.sight.controllers.http.dto

import java.time.Instant

data class ListSavedRecordResponse(
    val recordId: String,
    val type: String,
    val content: String,
    val groupId: String,
    val groupName: String,
    val cardId: String,
    val cardTitle: String,
    val isDeleted: Boolean,
    val isCardDeleted: Boolean,
    val authorName: String,
    val createdAt: Instant,
    val savedAt: Instant,
)

data class ListSavedRecordsResponse(
    val page: Int,
    val totalCount: Long,
    val receivedCount: Int,
    val records: List<ListSavedRecordResponse>,
)
