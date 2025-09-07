package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank

data class UpdateDiscordRoleRequest(
    @field:NotBlank(message = "Role ID is required")
    val roleId: String,
)
