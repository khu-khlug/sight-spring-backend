package com.sight.discord.service

import com.sight.repository.DiscordIntegrationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserService(
    private val discordIntegrationRepository: DiscordIntegrationRepository,
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun applyUserInfoToEnteredDiscordUser(discordUserId: String) {
        logger.info("Applying user info for Discord user: $discordUserId")

        val discordIntegration = discordIntegrationRepository.findByDiscordUserId(discordUserId)
        if (discordIntegration == null) {
            logger.info("No Discord integration found for user: $discordUserId")
            return
        }

        val userId = discordIntegration.userId
        logger.info("Found Discord integration for user ID: $userId, Discord user: $discordUserId")

        // TODO: Implement reflectUserInfoToDiscordUser logic
        // This would mirror the DiscordMemberService.reflectUserInfoToDiscordUser functionality
        // - Load user by userId
        // - Calculate roles based on user status
        // - Update Discord member nickname and roles
    }
}
