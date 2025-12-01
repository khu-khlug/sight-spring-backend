package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank

data class RejectGroupMatchingFieldRequestRequest(
    @field:NotBlank(message = "거절 사유는 필수입니다.")
    val rejectReason: String,
)
