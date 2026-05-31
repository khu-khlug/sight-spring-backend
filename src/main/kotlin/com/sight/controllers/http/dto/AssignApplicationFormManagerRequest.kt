package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class AssignApplicationFormManagerRequest(
    @field:NotBlank
    @field:Pattern(regexp = "\\d+")
    val managerUserId: String,
)
