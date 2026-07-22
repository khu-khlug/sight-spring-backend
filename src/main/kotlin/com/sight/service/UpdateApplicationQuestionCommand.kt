package com.sight.service

data class UpdateApplicationQuestionCommand(
    val id: String,
    val title: String,
    val description: String,
    val minLength: Int,
    val order: Int?,
    val isExposed: Boolean,
)
