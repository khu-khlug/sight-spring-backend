package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateNotificationRequest
import com.sight.controllers.http.dto.CreateNotificationResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.NotificationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalNotificationController(
    private val notificationService: NotificationService,
) {
    @Auth(roles = [UserRole.SYSTEM])
    @PostMapping("/internal/notifications")
    fun createNotification(
        @Valid @RequestBody request: CreateNotificationRequest,
    ): CreateNotificationResponse {
        val notification =
            notificationService.createNotification(
                userId = request.userId,
                category = request.category,
                title = request.title,
                content = request.content,
                url = request.url,
            )

        return CreateNotificationResponse(
            id = notification.id,
            userId = notification.userId,
            category = notification.category,
            title = notification.title,
            content = notification.content,
            url = notification.url,
            createdAt = notification.createdAt,
        )
    }
}
