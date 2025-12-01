package com.sight.service.dto

import java.time.LocalDateTime

data class GroupMatchingFieldDto(
    val id: String,
    val name: String,
    val createdAt: LocalDateTime,
)
