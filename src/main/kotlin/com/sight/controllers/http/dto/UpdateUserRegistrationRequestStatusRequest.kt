package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank

data class UpdateUserRegistrationRequestStatusRequest(
    @field:NotBlank
    val status: String,
)
