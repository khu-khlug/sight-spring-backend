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
import org.mockito.kotlin.argumentCaptor
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
    fun `기록 작성 시 본문이 비어 있거나 1 MiB를 넘으면 저장 요청을 거부한다`() {
        given(ids.nextId()).willReturn(4L)
        for (content in listOf("", MAX_CONTENT + "x")) {
            assertThrows<BadRequestException> { service.createGroupRecord(1L, 3L, 2L, content, emptyList()) }
        }
        verifyNoInteractions(records, attachments, logs, notifications, points)
    }

    @Test
    fun `기록 본문은 UTF-8 기준 1 MiB까지 저장할 수 있다`() {
        prepareRecordCreation()

        TransactionSynchronizationManager.initSynchronization()
        try {
            val result = service.createGroupRecord(1L, 3L, 2L, MAX_CONTENT, emptyList())

            assertEquals(MAX_CONTENT, result.content)
            val savedRecord = argumentCaptor<GroupRecord>()
            verify(records).save(savedRecord.capture())
            assertEquals(MAX_CONTENT, savedRecord.firstValue.content)
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `기록을 작성할 때 첨부 연결을 만든 뒤 같은 작성 대상의 임시저장을 삭제한다`() {
        val draft = GroupRecordDraft(5L, 2L, 1L, 3L, null, "draft", NOW)
        val files = listOf(RecordFileInput("block", "shared", null, null, null))
        prepareRecordCreation(files)
        given(drafts.findByMemberId(2L)).willReturn(draft)

        TransactionSynchronizationManager.initSynchronization()
        try {
            service.createGroupRecord(1L, 3L, 2L, "본문", files)

            inOrder(attachments, drafts) {
                verify(attachments).replaceRecordFiles(1L, 2L, 4L, files)
                verify(attachments).deleteDraftFiles(draft.id)
                verify(drafts).delete(draft)
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `기록을 작성하면 카드와 그룹의 기록 수가 증가하고 중단된 그룹이 진행 중으로 바뀐다`() {
        prepareRecordCreation()

        TransactionSynchronizationManager.initSynchronization()
        try {
            val result = service.createGroupRecord(1L, 3L, 2L, "본문", emptyList())

            assertEquals(4L, result.id)
            assertEquals(1L, card.recordCount)
            assertEquals(1L, group.recordCount)
            assertEquals(GroupStatus.PROGRESS, group.status)
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `기록 작성 트랜잭션이 롤백되면 경험치를 지급하지 않는다`() {
        prepareRecordCreation()

        TransactionSynchronizationManager.initSynchronization()
        try {
            service.createGroupRecord(1L, 3L, 2L, "본문", emptyList())

            val synchronization = TransactionSynchronizationManager.getSynchronizations().single()
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
            verifyNoInteractions(points)
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `본문과 첨부가 같아도 수정 시각과 활동 로그를 갱신하고 중단된 그룹을 진행 중으로 바꾼다`() {
        given(attachments.replaceRecordFiles(1L, 2L, 4L, emptyList())).willReturn(emptyList())

        service.updateGroupRecord(1L, 4L, 2L, "before", emptyList())

        assertNotEquals(NOW, record.updatedAt)
        assertEquals(GroupStatus.PROGRESS, group.status)
        assertEquals(record.updatedAt, group.lastActivityAt)
        verify(logs).createLog(1L, 2L, GroupLogData.RecordUpdated(4L, card.id, card.title))
        verifyNoInteractions(points)
    }

    @Test
    fun `기록 수정 시 본문이 비어 있거나 1 MiB를 넘으면 첨부를 처리하지 않고 요청을 거부한다`() {
        for (content in listOf("", MAX_CONTENT + "x")) {
            assertThrows<BadRequestException> { service.updateGroupRecord(1L, 4L, 2L, content, emptyList()) }
        }

        assertEquals("before", record.content)
        assertEquals(NOW, record.updatedAt)
        assertEquals(GroupStatus.STOP, group.status)
        verifyNoInteractions(attachments, drafts, logs, points)
    }

    @Test
    fun `기록을 수정해도 임시저장의 대상 카드가 현재 카드와 다르면 임시저장을 삭제하지 않는다`() {
        val previousCardDraft = GroupRecordDraft(5L, 2L, 1L, 9L, 4L, "draft", NOW)
        given(drafts.findByMemberId(2L)).willReturn(previousCardDraft)
        given(attachments.replaceRecordFiles(1L, 2L, 4L, emptyList())).willReturn(emptyList())

        service.updateGroupRecord(1L, 4L, 2L, "before", emptyList())

        assertEquals("before", record.content)
        assertNotEquals(NOW, record.updatedAt)
        assertEquals(GroupStatus.PROGRESS, group.status)
        verify(logs).createLog(1L, 2L, GroupLogData.RecordUpdated(4L, card.id, card.title))
        verify(attachments, never()).deleteDraftFiles(previousCardDraft.id)
        verify(drafts, never()).delete(previousCardDraft)
    }

    @Test
    fun `다른 회원이 작성한 기록은 수정할 수 없다`() {
        val other = GroupRecord(4L, card.id, 8L, "original", NOW)
        given(records.findLockedIncludingDeletedById(4L)).willReturn(other)

        assertThrows<ForbiddenException> { service.updateGroupRecord(1L, 4L, 2L, "changed", emptyList()) }

        assertEquals("original", other.content)
        verifyNoInteractions(attachments, drafts, logs, points)
    }

    private fun prepareRecordCreation(files: List<RecordFileInput> = emptyList()) {
        given(ids.nextId()).willReturn(4L)
        given(records.save(any<GroupRecord>())).willAnswer { it.getArgument(0) }
        given(attachments.replaceRecordFiles(1L, 2L, 4L, files)).willReturn(emptyList())
        given(groupMembers.findByGroupId(1L)).willReturn(emptyList())
        given(attachments.getRecordFiles(1L, 4L)).willReturn(emptyList())
    }

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
        private val MAX_CONTENT = "\uD83D\uDE00".repeat(1024 * 1024 / 4)
    }
}
