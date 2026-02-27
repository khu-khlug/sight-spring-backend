package com.sight.controllers.http

import com.sight.controllers.http.dto.CallbackDiscordIntegrationRequest
import com.sight.controllers.http.dto.GetCurrentUserResponse
import com.sight.controllers.http.dto.GetDiscordIntegrationResponse
import com.sight.controllers.http.dto.IssueDiscordIntegrationUrlResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.UserDiscordService
import com.sight.service.UserService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class UserController(
    private val userDiscordService: UserDiscordService,
    private val userService: UserService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/users/@me")
    fun getCurrentUser(requester: Requester): GetCurrentUserResponse {
        val member = userService.getMemberById(requester.userId)

        return GetCurrentUserResponse(
            id = member.id,
            name = member.name,
            manager = member.manager,
            status = member.status,
            studentStatus = member.studentStatus,
            createdAt = member.createdAt,
            updatedAt = member.updatedAt,
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/users/@me/graduate")
    fun graduateCurrentUser(requester: Requester): ResponseEntity<Void> {
        userService.graduateMember(requester.userId)

        // TODO: 추후 클라이언트에서 처리하도록 수정
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("https://khlug.org/my"))
            .build()
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @DeleteMapping("/users/@me")
    fun deleteCurrentUser(
        requester: Requester,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        userService.deleteMember(requester.userId)

        val cookie =
            Cookie("khlug_session", null).apply {
                path = "/"
                maxAge = 0
            }
        response.addCookie(cookie)

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("https://khlug.org/"))
            .build()
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/users/@me/check-first-today-login")
    fun checkFirstTodayLogin(requester: Requester): ResponseEntity<Void> {
        userService.checkFirstTodayLogin(requester.userId)

        return ResponseEntity.noContent().build()
    }

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

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/users/@me/discord-integration/issue-url")
    fun issueDiscordIntegrationUrl(requester: Requester): IssueDiscordIntegrationUrlResponse {
        val url = userDiscordService.issueDiscordIntegrationUrl(requester.userId)

        return IssueDiscordIntegrationUrlResponse(url)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @DeleteMapping("/users/@me/discord-integration")
    fun deleteCurrentUserDiscordIntegration(requester: Requester): ResponseEntity<Void> {
        userDiscordService.removeDiscordIntegration(requester.userId)

        return ResponseEntity.noContent().build()
    }
}
