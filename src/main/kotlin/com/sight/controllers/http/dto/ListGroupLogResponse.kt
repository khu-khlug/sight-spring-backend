package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class ListGroupLogResponse(
    val id: Long,
    val memberId: Long,
    val message: String,
    val createdAt: LocalDateTime,
)

data class ListGroupLogsResponse(
    val count: Long,
    val logs: List<ListGroupLogResponse>,
)
