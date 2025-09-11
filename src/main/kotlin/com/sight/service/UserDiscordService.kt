package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.discord.DiscordOAuth2Adapter
import com.sight.core.discord.DiscordStateGenerator
import com.sight.domain.discord.DiscordIntegration
import com.sight.repository.DiscordIntegrationRepository
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class UserDiscordService(
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val discordOAuth2Adapter: DiscordOAuth2Adapter,
    private val discordStateGenerator: DiscordStateGenerator,
    private val discordMemberService: DiscordMemberService,
) {
    fun getDiscordIntegration(userId: Long): DiscordIntegration {
        return discordIntegrationRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "아직 디스코드와 연동하지 않았습니다")
    }

    fun createDiscordIntegration(
        userId: Long,
        code: String,
        state: String,
    ) {
        val expectedState = discordStateGenerator.generate(userId)
        if (expectedState != state) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "알 수 없는 디스코드 OAuth2 상태값입니다")
        }

        val existingIntegration = discordIntegrationRepository.findByUserId(userId)
        if (existingIntegration != null) {
            // 이미 존재한다면 무시합니다.
            return
        }

        val accessToken = runBlocking { discordOAuth2Adapter.getAccessToken(code) }
        val discordUserId = runBlocking { discordOAuth2Adapter.getCurrentUserId(accessToken) }

        val newDiscordIntegration =
            DiscordIntegration(
                id = UlidCreator.getUlid().toString(),
                userId = userId,
                discordUserId = discordUserId,
                createdAt = LocalDateTime.now(),
            )
        discordIntegrationRepository.save(newDiscordIntegration)

        discordMemberService.reflectUserInfoToDiscordUser(userId)
    }
}
