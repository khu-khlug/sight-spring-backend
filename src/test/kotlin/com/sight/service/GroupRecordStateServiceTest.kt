package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.expoint.ExpointRule
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogExpointChange
import com.sight.domain.group.GroupLogType
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
import org.mockito.kotlin.verifyNoMoreInteractions
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
    fun `기록 삭제는 한 번만 반영하고 작성 또는 복구 때 지급한 경험치의 회수를 위임한다`() {
        card.increaseRecordCount(NOW)
        group.increaseRecordCount()
        willAnswer {
            record.delete(it.getArgument(1))
            null
        }.given(records).delete(eq(record), any())

        service.deleteGroupRecord(1L, 2L, 4L)
        assertThrows<BadRequestException> { service.deleteGroupRecord(1L, 2L, 4L) }

        assertTrue(record.isDeleted)
        assertEquals(0L, card.recordCount)
        assertEquals(0L, group.recordCount)
        assertEquals(GroupStatus.PROGRESS, group.status)
        verify(logs).revertExpointChange(
            groupId = 1L,
            actorMemberId = 2L,
            targetId = "4",
            targetLogTypes = setOf(GroupLogType.RECORD_CREATED, GroupLogType.RECORD_RESTORED),
            revertLog = GroupLogData.RecordDeleted(4L, card.id, card.title),
            message = "그룹 기록 삭제",
            fallbackRule = ExpointRule.기록삭제,
            fallbackMemberIds = listOf(8L),
        )
        verifyNoMoreInteractions(logs)
        verifyNoInteractions(points)
    }

    @Test
    fun `기록을 여러 번 복구해도 기록 수와 로그는 한 번만 갱신하고 경험치는 복구 요청자에게 지급한다`() {
        record.delete(NOW)
        TransactionSynchronizationManager.initSynchronization()
        try {
            repeat(2) { service.updateGroupRecordRestored(1L, 2L, 4L) }

            assertFalse(record.isDeleted)
            assertEquals(1L, card.recordCount)
            assertEquals(1L, group.recordCount)
            assertEquals(GroupStatus.PROGRESS, group.status)
            verify(logs).createLog(
                1L,
                2L,
                GroupLogData.RecordRestored(4L, card.id, card.title),
                GroupLogExpointChange(targetId = "4", point = 2, memberIds = listOf(2L)),
            )
            verifyNoInteractions(points)
            TransactionSynchronizationManager.getSynchronizations().single().afterCommit()
            verify(points).givePoint(2L, 2, "그룹 기록 복구")
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `현재 카드로 이동을 요청하면 기록과 그룹 상태 및 로그를 변경하지 않는다`() {
        card.increaseRecordCount(NOW)
        group.increaseRecordCount()

        service.updateGroupRecordPosition(1L, 2L, 4L, card.id)

        assertEquals(card.id, record.cardId)
        assertEquals(1L, card.recordCount)
        assertEquals(1L, group.recordCount)
        assertEquals("본문", record.content)
        assertEquals(GroupStatus.STOP, group.status)
        verifyNoInteractions(logs, points)
    }

    @Test
    fun `다른 그룹의 카드로는 기록을 이동할 수 없다`() {
        val foreign = GroupCard(9L, 10L, 11L, 2L, "다른 그룹 카드", 1L, NOW)
        given(cards.findById(foreign.id)).willReturn(Optional.of(foreign))
        card.increaseRecordCount(NOW)
        group.increaseRecordCount()

        assertThrows<BadRequestException> { service.updateGroupRecordPosition(1L, 2L, 4L, foreign.id) }

        assertEquals(card.id, record.cardId)
        assertEquals(1L, card.recordCount)
        assertEquals(0L, foreign.recordCount)
        assertEquals(1L, group.recordCount)
        assertEquals(GroupStatus.STOP, group.status)
        verifyNoInteractions(logs, points)
    }

    @Test
    fun `기록을 다른 카드로 옮기면 두 카드의 기록 수를 갱신하고 그룹의 기록 수와 본문은 유지한다`() {
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
    fun `중단된 그룹에서는 기록의 포트폴리오 포함 여부를 변경할 수 없다`() {
        assertThrows<BadRequestException> { service.updateGroupRecordPortfolio(1L, 2L, 4L, false) }

        assertTrue(record.isInPortfolio)
        verifyNoInteractions(logs)
    }

    @Test
    fun `진행 중인 그룹에서 기록을 포트폴리오에서 제외하면 포함 여부를 변경하고 로그를 남긴다`() {
        group.resumeByActivity()

        service.updateGroupRecordPortfolio(1L, 2L, 4L, false)

        assertFalse(record.isInPortfolio)
        verify(logs).createLog(1L, 2L, GroupLogData.RecordPortfolioRemoved(4L, card.id, card.title))
    }

    @Test
    fun `포트폴리오 포함 여부를 현재 값으로 요청하면 로그를 남기지 않는다`() {
        group.resumeByActivity()
        record.changePortfolio(false)

        service.updateGroupRecordPortfolio(1L, 2L, 4L, false)

        assertFalse(record.isInPortfolio)
        verifyNoInteractions(logs)
    }

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
    }
}
