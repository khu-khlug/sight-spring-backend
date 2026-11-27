package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.sight.service.dto.GroupCardSummaryResult
import com.sight.service.dto.GroupKanbanListsResult

data class ListGroupKanbanListsResponse(
    val page: Long,
    val totalCount: Long,
    val receivedCount: Int,
    val lists: List<ListGroupKanbanListResponse>,
) {
    companion object {
        fun from(
            groupId: Long,
            result: GroupKanbanListsResult,
        ): ListGroupKanbanListsResponse =
            ListGroupKanbanListsResponse(
                page = result.page,
                totalCount = result.totalCount,
                receivedCount = result.lists.size,
                lists =
                    result.lists.map { list ->
                        ListGroupKanbanListResponse(
                            id = list.id.toString(),
                            title = list.title,
                            description = list.description,
                            cardCount = list.cardCount,
                            cards = list.cards.map { GroupCardSummaryResponse.from(groupId, it) },
                        )
                    },
            )
    }
}

data class ListGroupKanbanListResponse(
    val id: String,
    val title: String,
    val description: String,
    val cardCount: Long,
    val cards: List<GroupCardSummaryResponse>,
)

data class GroupCardSummaryResponse(
    val id: String,
    val title: String,
    val authorName: String,
    val recordCount: Long,
    val hasDescription: Boolean,
    @get:JsonProperty("isInPortfolio") val isInPortfolio: Boolean,
    val assigneeUserId: Long?,
    val assigneeName: String?,
    val coverImageUrl: String?,
    val labels: List<String>,
    @get:JsonProperty("isDisabled") val isDisabled: Boolean,
    val createdAt: String,
) {
    companion object {
        fun from(
            groupId: Long,
            card: GroupCardSummaryResult,
        ): GroupCardSummaryResponse =
            GroupCardSummaryResponse(
                id = card.id.toString(),
                title = card.title,
                authorName = card.authorName,
                recordCount = card.recordCount,
                hasDescription = card.hasDescription,
                isInPortfolio = card.isInPortfolio,
                assigneeUserId = card.assigneeUserId,
                assigneeName = card.assigneeName,
                coverImageUrl = card.coverFileId?.let { "/groups/$groupId/files/$it" },
                labels = card.labels.map { if (it == "LIGHT_GRAY") "lightGray" else it.lowercase() },
                isDisabled = card.isDisabled,
                createdAt = card.createdAt.toString(),
            )
    }
}
