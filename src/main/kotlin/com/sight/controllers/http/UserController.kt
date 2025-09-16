package com.sight.controllers.http

import com.sight.controllers.http.dto.CallbackDiscordIntegrationRequest
import com.sight.controllers.http.dto.GetDiscordIntegrationResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.UserDiscordService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RestController
import java.net.URI

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

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/users/@me/discord-integration/callback")
    fun callbackDiscordIntegration(
        requester: Requester,
        @Valid @ModelAttribute request: CallbackDiscordIntegrationRequest,
    ): ResponseEntity<Void> {
        userDiscordService.createDiscordIntegration(
            userId = requester.userId,
            code = request.code,
            state = request.state,
        )

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("https://app.khlug.org/member/integrate-discord"))
            .build()
    }
}
