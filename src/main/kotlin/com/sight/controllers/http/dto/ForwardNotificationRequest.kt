package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class ForwardNotificationRequest(
    @field:NotBlank(message = "앱 이름은 필수입니다")
    @field:JsonProperty("appName")
    val appName: String,
    @field:NotBlank(message = "알림 제목은 필수입니다")
    @field:JsonProperty("title")
    val title: String,
    @field:NotBlank(message = "알림 내용은 필수입니다")
    @field:JsonProperty("content")
    val content: String,
    @field:JsonProperty("receivedAt")
    val receivedAt: Instant,
)
