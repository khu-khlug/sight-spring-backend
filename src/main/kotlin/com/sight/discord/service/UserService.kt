package com.sight.discord.service

import com.sight.repository.DiscordIntegrationRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val discordMemberService: DiscordMemberService,
) {
    fun applyUserInfoToEnteredDiscordUser(discordUserId: String) {
        val discordIntegration = discordIntegrationRepository.findByDiscordUserId(discordUserId) ?: return

        val userId = discordIntegration.userId
        discordMemberService.reflectUserInfoToDiscordUser(userId)
    }
}
