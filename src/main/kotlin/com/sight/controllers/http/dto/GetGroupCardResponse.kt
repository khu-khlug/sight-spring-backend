package com.sight.controllers.http.dto

import java.time.Instant

data class GetGroupCardResponse(
    val id: String,
    val title: String,
    val authorName: String,
    val recordCount: Long,
    val hasDescription: Boolean,
    val isInPortfolio: Boolean,
    val assigneeUserId: Long?,
    val assigneeName: String?,
    val coverImageUrl: String?,
    val labels: List<String>,
    val isDisabled: Boolean,
    val createdAt: Instant,
    val listId: String,
    val listTitle: String,
    val description: String,
    val recordSort: String,
)
