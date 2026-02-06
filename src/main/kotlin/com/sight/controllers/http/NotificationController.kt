package com.sight.controllers.http

import com.sight.controllers.http.dto.ListNotificationResponse
import com.sight.controllers.http.dto.ListNotificationsResponse
import com.sight.controllers.http.dto.ReadNotificationsRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.NotificationService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class NotificationController(
    private val notificationService: NotificationService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/notifications")
    fun listNotifications(
        requester: Requester,
        @RequestParam(defaultValue = "0") @Min(0) offset: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) limit: Int,
    ): ListNotificationsResponse {
        val result =
            notificationService.listNotifications(
                userId = requester.userId,
                offset = offset,
                limit = limit,
            )

        return ListNotificationsResponse(
            count = result.count,
            notifications =
                result.notifications.map { notification ->
                    ListNotificationResponse(
                        id = notification.id,
                        category = notification.category,
                        title = notification.title,
                        content = notification.content,
                        url = notification.url,
                        readAt = notification.readAt,
                        createdAt = notification.createdAt,
                    )
                },
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/notifications/read")
    fun readNotifications(
        requester: Requester,
        @Valid @RequestBody request: ReadNotificationsRequest,
    ) {
        notificationService.readNotifications(
            userId = requester.userId,
            notificationIds = request.notificationIds,
        )
    }
}
