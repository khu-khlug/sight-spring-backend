package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank

data class CreateUserRegistrationRequest(
    @field:NotBlank
    val info21Id: String,

    @field:NotBlank
    val info21Password: String,
)
