package com.sight.controllers.http

import com.sight.controllers.http.dto.GetDiscordIntegrationResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.UserDiscordService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userDiscordService: UserDiscordService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/users/@me/discord-integration")
    fun getCurrentUserDiscordIntegration(requester: Requester): GetDiscordIntegrationResponse {
        val integration = userDiscordService.getDiscordIntegration(requester.userId)

        return GetDiscordIntegrationResponse(
            id = integration.id,
            discordUserId = integration.discordUserId,
            createdAt = integration.createdAt,
        )
    }
}
