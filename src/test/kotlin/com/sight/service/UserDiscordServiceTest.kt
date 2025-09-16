package com.sight.service

import com.sight.core.discord.DiscordOAuth2Adapter
import com.sight.core.discord.DiscordStateGenerator
import com.sight.domain.discord.DiscordIntegration
import com.sight.repository.DiscordIntegrationRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import kotlin.test.assertEquals

class UserDiscordServiceTest {
    private val discordIntegrationRepository = mock<DiscordIntegrationRepository>()
    private val discordOAuth2Adapter = mock<DiscordOAuth2Adapter>()
    private val discordStateGenerator = mock<DiscordStateGenerator>()
    private val discordMemberService = mock<DiscordMemberService>()
    private val userDiscordService =
        UserDiscordService(
            discordIntegrationRepository,
            discordOAuth2Adapter,
            discordStateGenerator,
            discordMemberService,
        )

    @Test
    fun `사용자 ID로 디스코드 연동 정보를 조회한다`() {
        // given
        val userId = 123L
        val integration =
            DiscordIntegration(
                id = "test-id",
                userId = userId,
                discordUserId = "discord-123",
                createdAt = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            )
        given(discordIntegrationRepository.findByUserId(userId)).willReturn(integration)

        // when
        val result = userDiscordService.getDiscordIntegration(userId)

        // then
        assertEquals(integration, result)
    }

    @Test
    fun `디스코드 연동 정보가 없으면 404 예외를 던진다`() {
        // given
        val userId = 123L
        given(discordIntegrationRepository.findByUserId(userId)).willReturn(null)

        // when & then
        val exception =
            assertThrows<ResponseStatusException> {
                userDiscordService.getDiscordIntegration(userId)
            }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `디스코드 연동 URL을 발급한다`() {
        // given
        val userId = 123L
        val state = "generated-state"
        val expectedUrl =
            "https://discord.com/oauth2/authorize?" +
                "client_id=test&redirect_uri=http%3A//localhost%3A8080/callback&response_type=code&scope=identify&state=generated-state"

        given(discordStateGenerator.generate(userId)).willReturn(state)
        given(discordOAuth2Adapter.createOAuth2Url(state)).willReturn(expectedUrl)

        // when
        val result = userDiscordService.issueDiscordIntegrationUrl(userId)

        // then
        assertEquals(expectedUrl, result)
    }
}
