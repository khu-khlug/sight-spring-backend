package com.sight.core.discord

import com.sight.domain.discord.DiscordRole

data class DiscordApiModifyMemberParams(
    val discordUserId: String,
    val nickname: String? = null,
    val roles: List<DiscordRole>? = null,
)

interface DiscordApiAdapter {
    fun hasMember(discordUserId: String): Boolean

    fun modifyMember(params: DiscordApiModifyMemberParams)
}
