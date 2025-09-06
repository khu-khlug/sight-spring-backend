package com.sight.controllers.discord

import com.sight.core.discord.UserDiscordEventHandler
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class UserDiscordEventController(
    private val userDiscordEventHandler: UserDiscordEventHandler,
) : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val discordUserId = event.user.id
        userDiscordEventHandler.handleGuildMemberJoin(discordUserId)
    }
}
