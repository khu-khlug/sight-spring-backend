package com.sight.controllers.http.dto

import java.time.Instant

data class ListGroupLogActorResponse(
    val userId: Long,
    val name: String,
)

data class ListGroupLogResponse(
    val id: Long,
    val actor: ListGroupLogActorResponse?,
    val logType: String,
    val logData: Map<String, Any?>,
    val createdAt: Instant,
)

data class ListGroupLogsResponse(
    val skip: Int,
    val totalCount: Long,
    val receivedCount: Int,
    val logs: List<ListGroupLogResponse>,
)
