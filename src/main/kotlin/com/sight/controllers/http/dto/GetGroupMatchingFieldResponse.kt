package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class GetGroupMatchingFieldResponse(
    val id: String,
    val name: String,
    val createdAt: LocalDateTime,
    val obsoletedAt: LocalDateTime?,
)
