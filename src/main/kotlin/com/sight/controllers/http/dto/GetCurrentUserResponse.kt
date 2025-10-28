package com.sight.controllers.http.dto

import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import java.time.LocalDateTime

data class GetCurrentUserResponse(
    val id: Long,
    val name: String,
    val manager: Boolean,
    val status: UserStatus,
    val studentStatus: StudentStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
