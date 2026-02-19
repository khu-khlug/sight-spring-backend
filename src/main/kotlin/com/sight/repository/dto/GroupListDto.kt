package com.sight.repository.dto

import java.time.LocalDateTime

data class GroupListDto(
    val id: Long,
    val category: String,
    val title: String,
    val state: String,
    val countMember: Long,
    val allowJoin: Boolean,
    val createdAt: LocalDateTime,
    val leaderUserId: Long,
    val leaderName: String,
)
