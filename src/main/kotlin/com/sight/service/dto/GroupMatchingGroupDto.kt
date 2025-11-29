package com.sight.service.dto

import java.time.LocalDateTime

data class GroupMatchingGroupDto(
    val id: Long,
    val title: String,
    val members: List<GroupMatchingGroupMemberDto>,
    val createdAt: LocalDateTime,
)

data class GroupMatchingGroupMemberDto(
    val id: Long,
    val userId: Long,
    val name: String,
    val number: Long,
)
