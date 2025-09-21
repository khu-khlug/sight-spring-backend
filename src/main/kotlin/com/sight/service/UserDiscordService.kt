package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.discord.DiscordOAuth2Adapter
import com.sight.core.discord.DiscordStateGenerator
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.discord.DiscordIntegration
import com.sight.repository.DiscordIntegrationRepository
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
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
            ?: throw NotFoundException("아직 디스코드와 연동하지 않았습니다")
    }

    fun createDiscordIntegration(
        userId: Long,
        code: String,
        state: String,
    ) {
        val expectedState = discordStateGenerator.generate(userId)
        if (expectedState != state) {
            throw ForbiddenException("알 수 없는 디스코드 OAuth2 상태값입니다")
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

    fun removeDiscordIntegration(userId: Long) {
        discordIntegrationRepository.findByUserId(userId)
            ?: throw NotFoundException("아직 디스코드와 연동하지 않았습니다")

        discordMemberService.clearDiscordIntegration(userId)
    }

    fun issueDiscordIntegrationUrl(userId: Long): String {
        val state = discordStateGenerator.generate(userId)
        return discordOAuth2Adapter.createOAuth2Url(state)
    }
}
