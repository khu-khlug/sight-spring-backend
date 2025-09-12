package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank

data class CallbackDiscordIntegrationRequest(
    @field:NotBlank(message = "인증 코드는 필수입니다")
    val code: String,

    @field:NotBlank(message = "상태값은 필수입니다")
    val state: String,
)
