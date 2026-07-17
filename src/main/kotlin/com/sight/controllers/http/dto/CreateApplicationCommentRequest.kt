package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank

data class CreateApplicationCommentRequest(
    @field:NotBlank
    val content: String,
)
