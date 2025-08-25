package com.sight.discord.handler

import com.sight.discord.service.UserService
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class UserDiscordEventHandler(
    private val userService: UserService,
) : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val discordUserId = event.user.id
        userService.applyUserInfoToEnteredDiscordUser(discordUserId)
    }
}
