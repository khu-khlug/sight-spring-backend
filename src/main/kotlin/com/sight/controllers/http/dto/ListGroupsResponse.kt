package com.sight.controllers.http.dto

import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupState
import java.time.LocalDateTime

data class ListGroupsResponse(
    val count: Long,
    val groups: List<GroupResponse>,
)

data class GroupLeaderResponse(
    val userId: Long,
    val name: String,
)

data class GroupResponse(
    val id: Long,
    val category: GroupCategory,
    val title: String,
    val state: GroupState,
    val countMember: Long,
    val allowJoin: Boolean,
    val createdAt: LocalDateTime,
    val leader: GroupLeaderResponse,
)
