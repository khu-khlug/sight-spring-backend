package com.sight.service.discord

interface DiscordMessageSender {
    fun sendDirectMessage(
        discordUserId: String,
        content: String,
    )
}
