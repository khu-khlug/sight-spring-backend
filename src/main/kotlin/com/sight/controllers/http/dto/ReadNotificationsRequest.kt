package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class ReadNotificationsRequest(
    @field:NotEmpty(message = "알림 ID 목록은 비어있을 수 없습니다")
    @field:Size(max = 100, message = "한 번에 최대 100개의 알림만 읽음 처리할 수 있습니다")
    val notificationIds: List<String>,
)
