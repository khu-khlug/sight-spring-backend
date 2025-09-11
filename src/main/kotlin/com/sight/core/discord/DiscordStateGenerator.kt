package com.sight.core.discord

interface DiscordStateGenerator {
    fun generate(userId: Long): String

    fun validate(
        userId: Long,
        state: String,
    ): Boolean
}
