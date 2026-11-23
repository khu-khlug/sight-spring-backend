package com.sight.repository

import java.time.Instant

data class GroupKanbanListView(
    val id: Long,
    val title: String,
    val description: String,
)

data class GroupCardSummaryView(
    val listId: Long,
    val cardCount: Long,
    val id: Long,
    val title: String,
    val authorName: String,
    val recordCount: Long,
    val hasDescription: Boolean,
    val isInPortfolio: Boolean,
    val assigneeUserId: Long?,
    val assigneeName: String?,
    val coverFileId: String?,
    val labelBits: Int,
    val isDisabled: Boolean,
    val createdAt: Instant,
)

interface GroupKanbanQueryRepository {
    fun countListsByGroupId(groupId: Long): Long

    fun findListsByGroupIdOrderByPosition(
        groupId: Long,
        offset: Long,
        limit: Int,
    ): List<GroupKanbanListView>

    fun findCardSummariesByListIdsOrderByPosition(
        groupId: Long,
        listIds: List<Long>,
        offset: Long,
        limit: Int,
    ): List<GroupCardSummaryView>
}
