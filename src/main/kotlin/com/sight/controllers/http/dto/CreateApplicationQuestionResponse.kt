package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class CreateApplicationQuestionResponse(
    val id: String,
    val title: String,
    val description: String,
    val minLength: Int,
    val order: Int?,
    val isExposed: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
