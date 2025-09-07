package com.sight.discord.adapter

import com.sight.discord.model.DiscordRole

data class DiscordApiModifyMemberParams(
    val discordUserId: String,
    val nickname: String? = null,
    val roles: List<DiscordRole>? = null,
)

interface DiscordApiAdapter {
    fun hasMember(discordUserId: String): Boolean

    fun modifyMember(params: DiscordApiModifyMemberParams)
}
