package com.sight.service

import com.sight.core.discord.DiscordOAuth2Adapter
import com.sight.core.discord.DiscordStateGenerator
import com.sight.domain.discord.DiscordIntegration
import com.sight.repository.DiscordIntegrationRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals

class UserDiscordServiceCreateDiscordIntegrationTest {
    private lateinit var discordIntegrationRepository: DiscordIntegrationRepository
    private lateinit var discordOAuth2Adapter: DiscordOAuth2Adapter
    private lateinit var discordStateGenerator: DiscordStateGenerator
    private lateinit var discordMemberService: DiscordMemberService
    private lateinit var userDiscordService: UserDiscordService

    @BeforeEach
    fun setUp() {
        discordIntegrationRepository = mock()
        discordOAuth2Adapter = mock()
        discordStateGenerator = mock()
        discordMemberService = mock()
        userDiscordService =
            UserDiscordService(
                discordIntegrationRepository,
                discordOAuth2Adapter,
                discordStateGenerator,
                discordMemberService,
            )
    }

    @Test
    fun `잘못된 상태값이면 예외를 던진다`() {
        // Given
        val userId = 1L
        val code = "test-code"
        val state = "invalid-state"

        whenever(discordStateGenerator.validate(userId, state)).thenReturn(false)

        // When & Then
        val exception =
            assertThrows<ResponseStatusException> {
                userDiscordService.createDiscordIntegration(userId, code, state)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertEquals("잘못된 상태값입니다", exception.reason)
    }

    @Test
    fun `이미 연동이 있으면 무시한다`() {
        // Given
        val userId = 1L
        val code = "test-code"
        val state = "valid-state"
        val existingIntegration =
            DiscordIntegration(
                id = "existing-id",
                userId = userId,
                discordUserId = "discord-user-id",
                createdAt = java.time.LocalDateTime.now(),
            )

        whenever(discordStateGenerator.validate(userId, state)).thenReturn(true)
        whenever(discordIntegrationRepository.findByUserId(userId)).thenReturn(existingIntegration)

        // When
        userDiscordService.createDiscordIntegration(userId, code, state)

        // Then
        verify(discordIntegrationRepository, never()).save(any())
        verify(discordMemberService, never()).reflectUserInfoToDiscordUser(any())
    }

    @Test
    fun `새 연동을 성공적으로 생성한다`() {
        // Given
        val userId = 1L
        val code = "test-code"
        val state = "valid-state"
        val accessToken = "access-token"
        val discordUserId = "discord-user-id"

        whenever(discordStateGenerator.validate(userId, state)).thenReturn(true)
        whenever(discordIntegrationRepository.findByUserId(userId)).thenReturn(null)
        whenever(runBlocking { discordOAuth2Adapter.getAccessToken(code) }).thenReturn(accessToken)
        whenever(runBlocking { discordOAuth2Adapter.getCurrentUserId(accessToken) }).thenReturn(discordUserId)

        // When
        userDiscordService.createDiscordIntegration(userId, code, state)

        // Then
        verify(discordIntegrationRepository).save(any<DiscordIntegration>())
        verify(discordMemberService).reflectUserInfoToDiscordUser(userId)
    }
}
