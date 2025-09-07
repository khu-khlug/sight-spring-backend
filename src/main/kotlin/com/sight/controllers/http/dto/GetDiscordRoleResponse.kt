package com.sight.controllers.http.dto

import com.sight.domain.discord.DiscordRoleType
import java.time.LocalDateTime

data class GetDiscordRoleResponse(
    val id: Long,
    val roleType: DiscordRoleType,
    val roleId: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
