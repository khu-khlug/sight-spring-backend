package com.sight.controllers.http.dto

data class ListGroupRecordsResponse(
    val offset: Long,
    val totalCount: Long,
    val receivedCount: Int,
    val records: List<GroupRecordResponse>,
)
