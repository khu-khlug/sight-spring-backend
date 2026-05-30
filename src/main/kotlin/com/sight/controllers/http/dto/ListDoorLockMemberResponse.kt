package com.sight.controllers.http.dto

import com.sight.domain.member.Member

data class ListDoorLockMemberResponse(
    val count: Long,
    val members: List<DoorLockMemberResponse>,
)

data class DoorLockMemberResponse(
    val name: String,
    val number: Long,
)

fun Member.toDoorLockMemberResponse(): DoorLockMemberResponse =
    DoorLockMemberResponse(
        name = realname,
        number = number!!,
    )
