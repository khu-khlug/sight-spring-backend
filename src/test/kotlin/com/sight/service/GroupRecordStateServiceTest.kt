package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupRecordRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.willAnswer
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.Optional

class GroupRecordStateServiceTest {
    private val records = mock<GroupRecordRepository>()
    private val cards = mock<GroupCardRepository>()
    private val logs = mock<GroupLogService>()
    private val groups = mock<GroupRepository>()
    private val members = mock<MemberRepository>()
    private val points = mock<PointService>()
    private val support = GroupRecordMutationSupport(members, groups, records, cards, logs, points)
    private val service = GroupRecordStateService(support, records, cards, logs)
    private val group = testGroup(status = GroupStatus.STOP)
    private val card = GroupCard(3L, 1L, 6L, 2L, "원본 카드", 1L, NOW)
    private val record = GroupRecord(4L, card.id, 8L, "본문", NOW)

    @BeforeEach
    fun setUp() {
        given(members.findForUpdateById(2L)).willReturn(mock())
        given(groups.findLockedById(1L)).willReturn(group)
        given(records.findLockedIncludingDeletedById(4L)).willReturn(record)
        given(cards.findById(card.id)).willReturn(Optional.of(card))
    }

    @Test
    fun `삭제를 반복해도 카운트와 로그가 한 번만 바뀌고 작성자 경험치는 커밋 뒤 차감된다`() {
        card.increaseRecordCount(NOW)
        group.increaseRecordCount()
        willAnswer {
            record.delete(it.getArgument(1))
            null
        }.given(records).delete(eq(record), any())

        TransactionSynchronizationManager.initSynchronization()
        try {
            service.deleteGroupRecord(1L, 2L, 4L)
            assertThrows<BadRequestException> { service.deleteGroupRecord(1L, 2L, 4L) }

            assertTrue(record.isDeleted)
            assertEquals(0L, card.recordCount)
            assertEquals(0L, group.recordCount)
            assertEquals(GroupStatus.PROGRESS, group.status)
            verify(logs).createLog(1L, 2L, GroupLogData.RecordDeleted(4L, card.id, card.title))
            verifyNoInteractions(points)
            TransactionSynchronizationManager.getSynchronizations().single().afterCommit()
            verify(points).givePoint(8L, -2, "그룹 기록 삭제")
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `복구를 반복해도 카운트와 로그가 한 번만 바뀌고 복구한 회원에게 경험치를 준다`() {
        record.delete(NOW)
        TransactionSynchronizationManager.initSynchronization()
        try {
            repeat(2) { service.updateGroupRecordRestored(1L, 2L, 4L) }

            assertFalse(record.isDeleted)
            assertEquals(1L, card.recordCount)
            assertEquals(1L, group.recordCount)
            assertEquals(GroupStatus.PROGRESS, group.status)
            verify(logs).createLog(1L, 2L, GroupLogData.RecordRestored(4L, card.id, card.title))
            verifyNoInteractions(points)
            TransactionSynchronizationManager.getSynchronizations().single().afterCommit()
            verify(points).givePoint(2L, 2, "그룹 기록 복구")
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `같은 카드로 이동하거나 다른 그룹으로 이동을 시도해도 원본을 보존한다`() {
        val foreign = GroupCard(9L, 10L, 11L, 2L, "다른 그룹 카드", 1L, NOW)
        given(cards.findById(foreign.id)).willReturn(Optional.of(foreign))
        card.increaseRecordCount(NOW)

        service.updateGroupRecordPosition(1L, 2L, 4L, card.id)
        assertThrows<BadRequestException> { service.updateGroupRecordPosition(1L, 2L, 4L, foreign.id) }

        assertEquals(card.id, record.cardId)
        assertEquals(1L, card.recordCount)
        assertEquals(0L, foreign.recordCount)
        assertEquals(GroupStatus.STOP, group.status)
        verifyNoInteractions(logs, points)
    }

    @Test
    fun `이동하면 두 카드의 기록 수만 재배분하고 그룹의 기록 수와 본문을 보존한다`() {
        val target = GroupCard(5L, 1L, 6L, 2L, "목적지 카드", 2L, NOW)
        given(cards.findById(target.id)).willReturn(Optional.of(target))
        card.increaseRecordCount(NOW)
        group.increaseRecordCount()

        service.updateGroupRecordPosition(1L, 2L, 4L, target.id)

        assertEquals(target.id, record.cardId)
        assertEquals(0L, card.recordCount)
        assertEquals(1L, target.recordCount)
        assertEquals(1L, group.recordCount)
        assertEquals("본문", record.content)
        verify(logs).createLog(1L, 2L, GroupLogData.RecordMoved(4L, card.id, card.title, target.id, target.title))
    }

    @Test
    fun `중단된 그룹은 포트폴리오를 변경할 수 없고 같은 값을 다시 보내면 로그가 추가되지 않는다`() {
        assertThrows<BadRequestException> { service.updateGroupRecordPortfolio(1L, 2L, 4L, false) }
        assertTrue(record.isInPortfolio)
        verifyNoInteractions(logs)

        group.resumeByActivity()
        repeat(2) { service.updateGroupRecordPortfolio(1L, 2L, 4L, false) }

        assertFalse(record.isInPortfolio)
        verify(logs).createLog(1L, 2L, GroupLogData.RecordPortfolioRemoved(4L, card.id, card.title))
    }

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
    }
}
