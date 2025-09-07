package com.sight.service

import com.sight.domain.discord.DiscordIntegration
import com.sight.repository.DiscordIntegrationRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserDiscordServiceTest {
    private val discordIntegrationRepository = mock<DiscordIntegrationRepository>()
    private val userDiscordService = UserDiscordService(discordIntegrationRepository)

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
    fun `디스코드 연동 정보가 없으면 null을 반환한다`() {
        // given
        val userId = 123L
        given(discordIntegrationRepository.findByUserId(userId)).willReturn(null)

        // when
        val result = userDiscordService.getDiscordIntegration(userId)

        // then
        assertNull(result)
    }
}
