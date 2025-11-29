package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank

data class CreateGroupMatchingFieldRequestRequest(
    @field:NotBlank
    val fieldName: String,
    @field:NotBlank
    val requestReason: String,
)
