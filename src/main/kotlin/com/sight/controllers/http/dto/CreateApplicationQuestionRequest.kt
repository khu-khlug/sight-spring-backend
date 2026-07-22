package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero

data class CreateApplicationQuestionRequest(
    @field:NotBlank
    val title: String,

    @field:NotBlank
    val description: String,

    @field:PositiveOrZero
    val minLength: Int,
)
