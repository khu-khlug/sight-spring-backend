package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class GetOngoingGroupMatchingResponse(
    val id: String,
    val year: Int,
    val semester: Int,
    val closedAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
