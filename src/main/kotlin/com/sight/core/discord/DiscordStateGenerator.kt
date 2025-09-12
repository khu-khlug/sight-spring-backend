package com.sight.core.discord

interface DiscordStateGenerator {
    fun generate(userId: Long): String
}
