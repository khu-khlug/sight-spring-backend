package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class CreateUserRegistrationResponse(
    val id: String,
    val requestedUserId: Long,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
