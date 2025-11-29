package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class GetGroupMatchingGroupsResponse(
    val id: Long,
    val title: String,
    val members: List<GroupMatchingGroupMemberResponse>,
    val createdAt: LocalDateTime,
)

data class GroupMatchingGroupMemberResponse(
    val id: Long,
    val userId: Long,
    val name: String,
    val number: Long,
)
