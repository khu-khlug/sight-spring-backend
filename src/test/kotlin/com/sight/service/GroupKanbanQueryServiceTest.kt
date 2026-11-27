package com.sight.service

import com.sight.repository.GroupKanbanListView
import com.sight.repository.GroupKanbanQueryRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupKanbanQueryServiceTest {
    private val repository = mock<GroupKanbanQueryRepository>()
    private val service = GroupKanbanQueryService(repository)

    @Test
    fun `두 번째 리스트 페이지에도 각 리스트의 카드는 처음부터 조회한다`() {
        given(repository.countListsByGroupId(1L)).willReturn(11L)
        given(repository.findListsByGroupIdOrderByPosition(1L, 10L, 10))
            .willReturn(listOf(GroupKanbanListView(20L, "리스트", "")))
        given(repository.findCardSummariesByListIdsOrderByPosition(1L, listOf(20L), 0L, 15))
            .willReturn(emptyList())

        val result = service.listGroupKanbanLists(1L, 1L)

        assertEquals(1L, result.page)
        assertEquals(11L, result.totalCount)
        assertEquals(20L, result.lists.single().id)
        assertEquals(0L, result.lists.single().cardCount)
        assertTrue(result.lists.single().cards.isEmpty())
        verify(repository).findCardSummariesByListIdsOrderByPosition(1L, listOf(20L), 0L, 15)
    }

    @Test
    fun `매우 큰 페이지 번호도 전체 개수와 빈 리스트를 반환한다`() {
        given(repository.countListsByGroupId(1L)).willReturn(11L)

        val result = service.listGroupKanbanLists(1L, Long.MAX_VALUE)

        assertEquals(Long.MAX_VALUE, result.page)
        assertEquals(11L, result.totalCount)
        assertTrue(result.lists.isEmpty())
        verify(repository).countListsByGroupId(1L)
        verifyNoMoreInteractions(repository)
    }
}
