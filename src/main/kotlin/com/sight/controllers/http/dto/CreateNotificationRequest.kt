package com.sight.controllers.http.dto

import com.sight.domain.notification.NotificationCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateNotificationRequest(
    val userId: Long,
    val category: NotificationCategory,
    @field:Size(max = 200, message = "알림 제목은 200자를 초과할 수 없습니다")
    val title: String,
    @field:NotBlank(message = "알림 내용은 필수입니다")
    val content: String,
    val url: String? = null,
)
