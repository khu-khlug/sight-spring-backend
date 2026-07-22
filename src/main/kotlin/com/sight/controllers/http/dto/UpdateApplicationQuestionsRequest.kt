package com.sight.controllers.http.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

data class UpdateApplicationQuestionsRequest(
    @field:NotEmpty
    @field:Valid
    val questions: List<QuestionRequest>,
) {
    data class QuestionRequest(
        @field:NotBlank
        val id: String,

        @field:NotBlank
        val title: String,

        @field:NotBlank
        val description: String,

        @field:PositiveOrZero
        val minLength: Int,

        @field:Positive
        val order: Int?,

        val isExposed: Boolean,
    )
}
