package com.sight.service

import com.sight.domain.group.GroupCardLabel
import com.sight.repository.GroupKanbanQueryRepository
import com.sight.service.dto.GroupCardSummaryResult
import com.sight.service.dto.GroupKanbanListResult
import com.sight.service.dto.GroupKanbanListsResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupKanbanQueryService(private val repository: GroupKanbanQueryRepository) {
    @Transactional(readOnly = true)
    fun listGroupKanbanLists(
        groupId: Long,
        page: Long,
    ): GroupKanbanListsResult {
        val totalCount = repository.countListsByGroupId(groupId)
        if (totalCount == 0L || page > (totalCount - 1) / LIST_PAGE_SIZE) {
            return GroupKanbanListsResult(page, totalCount, emptyList())
        }

        val lists = repository.findListsByGroupIdOrderByPosition(groupId, page * LIST_PAGE_SIZE, LIST_PAGE_SIZE)
        if (lists.isEmpty()) return GroupKanbanListsResult(page, totalCount, emptyList())

        val cardsByListId =
            repository.findCardSummariesByListIdsOrderByPosition(groupId, lists.map { it.id }, 0, CARD_PAGE_SIZE)
                .groupBy { it.listId }

        return GroupKanbanListsResult(
            page = page,
            totalCount = totalCount,
            lists =
                lists.map { list ->
                    val cards = cardsByListId[list.id].orEmpty()
                    GroupKanbanListResult(
                        id = list.id,
                        title = list.title,
                        description = list.description,
                        cardCount = cards.firstOrNull()?.cardCount ?: 0,
                        cards =
                            cards.map { card ->
                                GroupCardSummaryResult(
                                    id = card.id,
                                    title = card.title,
                                    authorName = card.authorName,
                                    recordCount = card.recordCount,
                                    hasDescription = card.hasDescription,
                                    isInPortfolio = card.isInPortfolio,
                                    assigneeUserId = card.assigneeUserId,
                                    assigneeName = card.assigneeName,
                                    coverFileId = card.coverFileId,
                                    labels = GroupCardLabel.fromBits(card.labelBits).map { it.name },
                                    isDisabled = card.isDisabled,
                                    createdAt = card.createdAt,
                                )
                            },
                    )
                },
        )
    }

    private companion object {
        const val LIST_PAGE_SIZE = 10
        const val CARD_PAGE_SIZE = 15
    }
}
