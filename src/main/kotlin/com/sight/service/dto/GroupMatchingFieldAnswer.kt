package com.sight.service.dto

import java.time.LocalDateTime

data class GroupMatchingFieldAnswer(
    val id: String,
    val name: String,
    val createdAt: LocalDateTime,
    val obsoletedAt: LocalDateTime?,
)
