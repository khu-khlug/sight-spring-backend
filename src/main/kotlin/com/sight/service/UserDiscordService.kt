package com.sight.service

import com.sight.domain.discord.DiscordIntegration
import com.sight.repository.DiscordIntegrationRepository
import org.springframework.stereotype.Service

@Service
class UserDiscordService(
    private val discordIntegrationRepository: DiscordIntegrationRepository,
) {
    fun getDiscordIntegration(userId: Long): DiscordIntegration? {
        return discordIntegrationRepository.findByUserId(userId)
    }
}
