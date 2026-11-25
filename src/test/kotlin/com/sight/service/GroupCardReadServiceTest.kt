package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupCard
import com.sight.repository.GroupCardRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import java.time.Instant

class GroupCardReadServiceTest {
    private val cards = mock<GroupCardRepository>()
    private val service = GroupCardReadService(cards)

    @Test
    fun `정상 카드는 기본 정보를 반환하고 상태와 수정 시각을 바꾸지 않는다`() {
        val card = card()
        val before = snapshot(card)
        given(cards.findAvailableByIdAndGroupId(card.id, card.groupId)).willReturn(card)

        val result = service.getGroupCardById(card.groupId, card.id)

        assertEquals(GroupCardBasicResult(3L, 1L, "카드", false, NOW, NOW), result)
        assertEquals(before, snapshot(card))
        verify(cards).findAvailableByIdAndGroupId(card.id, card.groupId)
        verifyNoMoreInteractions(cards)
    }

    @Test
    fun `없는 카드는 NotFoundException을 던진다`() {
        given(cards.findAvailableByIdAndGroupId(3L, 1L)).willReturn(null)
        given(cards.existsDeletedByIdAndGroupId(3L, 1L)).willReturn(false)

        val exception = assertThrows<NotFoundException> { service.getGroupCardById(1L, 3L) }

        assertEquals("카드를 찾을 수 없습니다.", exception.message)
        verifyMissingLookup(3L, 1L)
    }

    @Test
    fun `다른 그룹 카드는 NotFoundException을 던진다`() {
        val card = card()
        given(cards.findAvailableByIdAndGroupId(card.id, 9L)).willReturn(null)
        given(cards.existsDeletedByIdAndGroupId(card.id, 9L)).willReturn(false)

        assertThrows<NotFoundException> { service.getGroupCardById(9L, card.id) }

        verifyMissingLookup(card.id, 9L)
    }

    @Test
    fun `같은 그룹의 삭제된 카드는 BadRequestException을 던진다`() {
        val card = card().apply { delete(NOW.plusSeconds(60)) }
        given(cards.findAvailableByIdAndGroupId(card.id, card.groupId)).willReturn(null)
        given(cards.existsDeletedByIdAndGroupId(card.id, card.groupId)).willReturn(true)

        val exception = assertThrows<BadRequestException> { service.getGroupCardById(card.groupId, card.id) }

        assertEquals("삭제된 카드입니다.", exception.message)
        verifyMissingLookup(card.id, card.groupId)
    }

    @Test
    fun `비활성 카드도 조회하고 상태와 수정 시각을 바꾸지 않는다`() {
        val changedAt = NOW.plusSeconds(60)
        val card = card().apply { changeDisabled(true, changedAt) }
        val before = snapshot(card)
        given(cards.findAvailableByIdAndGroupId(card.id, card.groupId)).willReturn(card)

        val result = service.getGroupCardById(card.groupId, card.id)

        assertEquals(GroupCardBasicResult(3L, 1L, "카드", true, NOW, changedAt), result)
        assertEquals(before, snapshot(card))
        verify(cards).findAvailableByIdAndGroupId(card.id, card.groupId)
        verifyNoMoreInteractions(cards)
    }

    @Test
    fun `다른 그룹의 삭제된 카드도 BadRequestException 대신 NotFoundException을 던진다`() {
        val card = card().apply { delete(NOW.plusSeconds(60)) }
        given(cards.findAvailableByIdAndGroupId(card.id, 9L)).willReturn(null)
        given(cards.existsDeletedByIdAndGroupId(card.id, 9L)).willReturn(false)

        assertThrows<NotFoundException> { service.getGroupCardById(9L, card.id) }

        verifyMissingLookup(card.id, 9L)
    }

    private fun verifyMissingLookup(
        cardId: Long,
        groupId: Long,
    ) {
        verify(cards).findAvailableByIdAndGroupId(cardId, groupId)
        verify(cards).existsDeletedByIdAndGroupId(cardId, groupId)
        verifyNoMoreInteractions(cards)
    }

    private fun snapshot(card: GroupCard): List<Any?> =
        listOf(
            card.listId, card.position, card.title, card.description, card.assigneeMemberId,
            card.labelBits, card.isInPortfolio, card.isDisabled, card.recordSort, card.recordCount,
            card.updatedAt, card.deletedAt,
        )

    private fun card() = GroupCard(3L, 1L, 5L, 2L, "카드", 1L, NOW)

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
    }
}
