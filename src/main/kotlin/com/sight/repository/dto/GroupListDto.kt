package com.sight.repository.dto

import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupStatus
import java.time.Instant

data class GroupListDto(
    val id: Long,
    val category: GroupCategory,
    val title: String,
    val status: GroupStatus,
    val memberCount: Long,
    val allowJoin: Boolean,
    val createdAt: Instant,
    val leaderUserId: Long,
    val leaderName: String,
)
