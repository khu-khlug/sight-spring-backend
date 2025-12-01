package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class GroupMatchingResponse(
    val groupMatchingId: String,
    val year: Int,
    val semester: Int,
    val closedAt: LocalDateTime,
    val createdAt: LocalDateTime,
)
