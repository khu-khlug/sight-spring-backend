package com.sight.controllers.http.dto

import com.sight.domain.notification.NotificationCategory
import java.time.LocalDateTime

data class ListNotificationResponse(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val content: String,
    val url: String?,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)

data class ListNotificationsResponse(
    val count: Long,
    val notifications: List<ListNotificationResponse>,
)
