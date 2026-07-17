package com.sight.controllers.http.dto

data class ListGroupMemberResponse(
    val userId: Long,
    val name: String,
    val realname: String,
    val isLeader: Boolean,
)

data class ListGroupMembersResponse(
    val members: List<ListGroupMemberResponse>,
)
