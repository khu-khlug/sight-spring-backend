package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class CreateGroupDiscordChannelResponse(
    val id: String,
    val groupId: String,
    val discordChannelId: String,
    val createdAt: LocalDateTime,
)
