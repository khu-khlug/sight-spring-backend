package com.sight.repository.dto

import java.time.LocalDateTime

data class GroupLogListDto(
    val id: Long,
    val memberId: Long,
    val message: String,
    val createdAt: LocalDateTime,
)
