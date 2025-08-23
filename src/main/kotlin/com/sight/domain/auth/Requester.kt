package com.sight.domain.auth

data class Requester(
    val userId: Long,
    val role: UserRole
)