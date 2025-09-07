package com.sight.controllers.http

import com.sight.controllers.http.dto.GetDiscordIntegrationResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.UserDiscordService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class UserController(
    private val userDiscordService: UserDiscordService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/users/@me/discord-integration")
    fun getCurrentUserDiscordIntegration(requester: Requester): GetDiscordIntegrationResponse {
        val integration =
            userDiscordService.getDiscordIntegration(requester.userId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "아직 디스코드와 연동하지 않았습니다")

        return GetDiscordIntegrationResponse(
            id = integration.id,
            discordUserId = integration.discordUserId,
            createdAt = integration.createdAt,
        )
    }
}
