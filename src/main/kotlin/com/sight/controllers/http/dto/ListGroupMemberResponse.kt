package com.sight.controllers.http.dto

data class ListGroupMemberResponse(
    val userId: Long,
    val name: String,
    val college: String,
    val cardCount: Long,
    val recordCount: Long,
)

data class ListGroupMembersResponse(
    val leaderUserId: Long?,
    val members: List<ListGroupMemberResponse>,
)
