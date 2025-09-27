package com.sight.service.discord

import com.sight.domain.discord.DiscordRoleType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

data class DiscordApiModifyMemberParams(
    val discordUserId: String,
    val nickname: String? = null,
    val roles: List<DiscordRoleType>? = null,
)

interface DiscordApiAdapter {
    fun hasMember(discordUserId: String): Boolean

    fun modifyMember(params: DiscordApiModifyMemberParams)

    fun createGroupTextChannel(channelName: String): TextChannel
}
