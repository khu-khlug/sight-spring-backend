package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupRecordDraft
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.domain.member.Member
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRecordDraftRepository
import com.sight.repository.GroupRecordRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.Optional

class GroupRecordWriteServiceTest {
    private val groups = mock<GroupRepository>()
    private val cards = mock<GroupCardRepository>()
    private val records = mock<GroupRecordRepository>()
    private val members = mock<MemberRepository>()
    private val groupMembers = mock<GroupMemberRepository>()
    private val drafts = mock<GroupRecordDraftRepository>()
    private val attachments = mock<GroupRecordAttachmentService>()
    private val ids = mock<IntegerIdService>()
    private val logs = mock<GroupLogService>()
    private val notifications = mock<NotificationService>()
    private val points = mock<PointService>()
    private val service =
        GroupRecordWriteService(groups, cards, records, members, groupMembers, drafts, attachments, ids, logs, notifications, points)
    private val group = testGroup(status = GroupStatus.STOP)
    private val card = GroupCard(3L, 1L, 6L, 2L, "카드", 1L, NOW)
    private val record = GroupRecord(4L, card.id, 2L, "before", NOW)

    @BeforeEach
    fun setUp() {
        val member = mock<Member>()
        given(member.realname).willReturn("작성자")
        given(members.findForUpdateById(2L)).willReturn(member)
        given(groups.findLockedById(1L)).willReturn(group)
        given(cards.findById(card.id)).willReturn(Optional.of(card))
        given(records.findLockedIncludingDeletedById(4L)).willReturn(record)
    }

    @Test
    fun `한 바이트 초과한 다중 바이트 본문과 빈 본문은 저장 전에 거부한다`() {
        given(ids.nextId()).willReturn(4L)
        for (content in listOf("", MAX_CONTENT + "x")) {
            assertThrows<BadRequestException> { service.createGroupRecord(1L, 3L, 2L, content, emptyList()) }
        }
        verifyNoInteractions(records, attachments, logs, notifications, points)
    }

    @Test
    fun `최대 바이트 본문을 저장하고 같은 대상의 임시저장을 새 첨부 연결 뒤에 지운다`() {
        val draft = GroupRecordDraft(5L, 2L, 1L, 3L, null, "draft", NOW)
        val files = listOf(RecordFileInput("block", "shared", null, null, null))
        given(ids.nextId()).willReturn(4L)
        given(records.save(any<GroupRecord>())).willAnswer { it.getArgument(0) }
        given(attachments.replaceRecordFiles(1L, 2L, 4L, files)).willReturn(RecordFilesChangeResult(emptyList(), true))
        given(drafts.findByMemberId(2L)).willReturn(draft)
        given(groupMembers.findByGroupId(1L)).willReturn(emptyList())
        given(attachments.getRecordFiles(1L, 4L)).willReturn(emptyList())

        TransactionSynchronizationManager.initSynchronization()
        try {
            val result = service.createGroupRecord(1L, 3L, 2L, MAX_CONTENT, files)

            assertEquals(MAX_CONTENT, result.content)
            assertEquals(4L, result.id)
            assertEquals(1L, card.recordCount)
            assertEquals(1L, group.recordCount)
            assertEquals(GroupStatus.PROGRESS, group.status)
            inOrder(attachments, drafts) {
                verify(attachments).replaceRecordFiles(1L, 2L, 4L, files)
                verify(attachments).deleteDraftFiles(draft.id)
                verify(drafts).delete(draft)
            }
            val synchronization = TransactionSynchronizationManager.getSynchronizations().single()
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
            verifyNoInteractions(points)
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `본문과 첨부가 같으면 기록 갱신 시각과 그룹 상태를 유지한다`() {
        given(attachments.replaceRecordFiles(1L, 2L, 4L, emptyList())).willReturn(RecordFilesChangeResult(emptyList(), false))

        service.updateGroupRecord(1L, 4L, 2L, "before", emptyList())

        assertEquals(NOW, record.updatedAt)
        assertEquals(GroupStatus.STOP, group.status)
        verifyNoInteractions(logs, points)
    }

    @Test
    fun `수정할 본문이 유효하지 않으면 첨부 처리 전에 거부한다`() {
        for (content in listOf("", MAX_CONTENT + "x")) {
            assertThrows<BadRequestException> { service.updateGroupRecord(1L, 4L, 2L, content, emptyList()) }
        }

        assertEquals("before", record.content)
        assertEquals(NOW, record.updatedAt)
        assertEquals(GroupStatus.STOP, group.status)
        verifyNoInteractions(attachments, drafts, logs, points)
    }

    @Test
    fun `첨부만 바꿔도 수정으로 기록하고 같은 기록의 이전 카드 임시저장은 보존한다`() {
        val previousCardDraft = GroupRecordDraft(5L, 2L, 1L, 9L, 4L, "draft", NOW)
        given(drafts.findByMemberId(2L)).willReturn(previousCardDraft)
        given(attachments.replaceRecordFiles(1L, 2L, 4L, emptyList())).willReturn(RecordFilesChangeResult(emptyList(), true))

        service.updateGroupRecord(1L, 4L, 2L, "before", emptyList())

        assertEquals("before", record.content)
        assertNotEquals(NOW, record.updatedAt)
        assertEquals(GroupStatus.PROGRESS, group.status)
        verify(logs).createLog(1L, 2L, GroupLogData.RecordUpdated(4L, card.id, card.title))
        verify(attachments, never()).deleteDraftFiles(previousCardDraft.id)
        verify(drafts, never()).delete(previousCardDraft)
    }

    @Test
    fun `다른 작성자의 기록은 본문이나 첨부를 바꾸지 않는다`() {
        val other = GroupRecord(4L, card.id, 8L, "original", NOW)
        given(records.findLockedIncludingDeletedById(4L)).willReturn(other)

        assertThrows<ForbiddenException> { service.updateGroupRecord(1L, 4L, 2L, "changed", emptyList()) }

        assertEquals("original", other.content)
        verifyNoInteractions(attachments, drafts, logs, points)
    }

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
        private val MAX_CONTENT = "\uD83D\uDE00".repeat(1024 * 1024 / 4)
    }
}
