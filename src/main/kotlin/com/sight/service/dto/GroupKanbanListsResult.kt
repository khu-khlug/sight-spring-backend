package com.sight.service.dto

import java.time.Instant

data class GroupKanbanListsResult(
    val page: Long,
    val totalCount: Long,
    val lists: List<GroupKanbanListResult>,
)

data class GroupKanbanListResult(
    val id: Long,
    val title: String,
    val description: String,
    val cardCount: Long,
    val cards: List<GroupCardSummaryResult>,
)

data class GroupCardSummaryResult(
    val id: Long,
    val title: String,
    val authorName: String,
    val recordCount: Long,
    val hasDescription: Boolean,
    val isInPortfolio: Boolean,
    val assigneeUserId: Long?,
    val assigneeName: String?,
    val coverFileId: String?,
    val labels: List<String>,
    val isDisabled: Boolean,
    val createdAt: Instant,
)
