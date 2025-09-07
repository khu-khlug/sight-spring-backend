package com.sight.core.auth

data class Requester(
    val userId: Long,
    val role: UserRole,
)
