package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class CreateApplicationCommentResponse(
    val id: String,
    val applicationFormId: String,
    val authorUserId: Long,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
