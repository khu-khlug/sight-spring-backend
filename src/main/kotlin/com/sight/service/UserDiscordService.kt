package com.sight.service

import com.sight.domain.discord.DiscordIntegration
import com.sight.repository.DiscordIntegrationRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserDiscordService(
    private val discordIntegrationRepository: DiscordIntegrationRepository,
) {
    fun getDiscordIntegration(userId: Long): DiscordIntegration {
        return discordIntegrationRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "아직 디스코드와 연동하지 않았습니다")
    }
}
