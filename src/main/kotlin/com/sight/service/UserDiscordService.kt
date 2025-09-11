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
        // 상태값 검증
        if (!discordStateGenerator.validate(userId, state)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "잘못된 상태값입니다")
        }

        // 이미 연동이 있는지 확인
        val existingIntegration = discordIntegrationRepository.findByUserId(userId)
        if (existingIntegration != null) {
            // 이미 존재한다면 무시합니다.
            return
        }

        // OAuth2 토큰 교환 및 사용자 정보 획득
        val accessToken = runBlocking { discordOAuth2Adapter.getAccessToken(code) }
        val discordUserId = runBlocking { discordOAuth2Adapter.getCurrentUserId(accessToken) }

        // 새 연동 정보 저장
        val newDiscordIntegration =
            DiscordIntegration(
                id = UlidCreator.getUlid().toString(),
                userId = userId,
                discordUserId = discordUserId,
                createdAt = LocalDateTime.now(),
            )
        discordIntegrationRepository.save(newDiscordIntegration)

        // 디스코드 멤버 정보 동기화
        discordMemberService.reflectUserInfoToDiscordUser(userId)
    }
}
