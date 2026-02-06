package com.sight.controllers.http.dto

import com.sight.domain.notification.NotificationCategory
import java.time.LocalDateTime

data class CreateNotificationResponse(
    val id: String,
    val userId: Long,
    val category: NotificationCategory,
    val title: String,
    val content: String,
    val url: String?,
    val createdAt: LocalDateTime,
)
