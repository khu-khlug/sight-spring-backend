package com.sight.repository.dto

data class GroupMemberListDto(
    val userId: Long,
    val name: String,
    val college: String,
    val cardCount: Long,
    val recordCount: Long,
)
