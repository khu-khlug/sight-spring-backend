package com.sight.service.discord

import com.sight.domain.discord.DiscordRoleType

data class DiscordApiModifyMemberParams(
    val discordUserId: String,
    val nickname: String? = null,
    val roles: List<DiscordRoleType>? = null,
)

interface DiscordApiAdapter {
    fun hasMember(discordUserId: String): Boolean

    fun modifyMember(params: DiscordApiModifyMemberParams)
}
