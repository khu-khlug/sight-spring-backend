package com.sight.controllers.http.dto

import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import java.time.Instant
import java.time.LocalDateTime

data class GetCurrentUserResponse(
    val id: Long,
    val name: String,
    val manager: Boolean,
    val status: UserStatus,
    val studentStatus: StudentStatus,
    val createdAt: Instant,
    val updatedAt: LocalDateTime,
)
