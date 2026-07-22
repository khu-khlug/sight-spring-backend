package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank

data class SubmitApplicationFormRequest(
    @field:NotBlank val token: String,
)
