package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class GetGroupDiscordChannelResponse(
    val id: String,
    val groupId: Long,
    val discordChannelId: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
