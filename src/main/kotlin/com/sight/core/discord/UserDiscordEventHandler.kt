package com.sight.core.discord

import com.sight.discord.service.UserService
import org.springframework.stereotype.Component

@Component
class UserDiscordEventHandler(
    private val userService: UserService,
) {
    fun handleGuildMemberJoin(discordUserId: String) {
        userService.applyUserInfoToEnteredDiscordUser(discordUserId)
    }
}
