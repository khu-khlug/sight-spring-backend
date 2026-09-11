package com.sight.controllers.http.dto

data class ListApplicationQuestionsResponse(
    val questions: List<QuestionResponse>,
) {
    data class QuestionResponse(
        val id: String,
        val title: String,
        val description: String,
        val minLength: Int,
        val order: Int?,
    )
}
