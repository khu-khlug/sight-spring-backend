package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class CreateGroupMatchingResponse(
    val id: String,
    val year: Int,
    val semester: Int,
    val createdAt: LocalDateTime,
    val closedAt: LocalDateTime,
)
