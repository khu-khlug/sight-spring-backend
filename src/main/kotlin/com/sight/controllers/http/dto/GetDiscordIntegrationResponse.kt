package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class GetDiscordIntegrationResponse(
    val id: String,
    val discordUserId: String,
    val createdAt: LocalDateTime,
)
