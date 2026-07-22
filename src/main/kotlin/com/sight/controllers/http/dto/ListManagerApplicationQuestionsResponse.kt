package com.sight.controllers.http.dto

data class ListManagerApplicationQuestionsResponse(
    val questions: List<QuestionResponse>,
) {
    data class QuestionResponse(
        val id: String,
        val title: String,
        val description: String,
        val minLength: Int,
        val order: Int?,
        val isExposed: Boolean,
    )
}
