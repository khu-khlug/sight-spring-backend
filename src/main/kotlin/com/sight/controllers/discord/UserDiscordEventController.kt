package com.sight.controllers.discord

import com.sight.core.discord.DiscordEventListener
import com.sight.service.UserService
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
@DiscordEventListener
class UserDiscordEventController(
    private val userService: UserService,
) : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val discordUserId = event.user.id
        userService.applyUserInfoToEnteredDiscordUser(discordUserId)
    }
}
