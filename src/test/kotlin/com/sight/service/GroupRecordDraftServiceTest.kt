package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ConflictException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupRecordDraft
import com.sight.domain.group.testGroup
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupRecordDraftRepository
import com.sight.repository.GroupRecordRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant
import java.util.Optional

class GroupRecordDraftServiceTest {
    private val drafts = mock<GroupRecordDraftRepository>()
    private val cards = mock<GroupCardRepository>()
    private val attachments = mock<GroupRecordAttachmentService>()
    private val groups = mock<GroupRepository>()
    private val records = mock<GroupRecordRepository>()
    private val members = mock<MemberRepository>()
    private val service = GroupRecordDraftService(drafts, cards, attachments, groups, records, members, mock())

    @Test
    fun `임시저장이 다른 그룹에 속하면 메타데이터 조회는 null을 반환한다`() {
        val draft = GroupRecordDraft(5L, 2L, 9L, 30L, null, "other", NOW)
        given(drafts.findByMemberId(2L)).willReturn(draft)

        assertNull(service.getGroupRecordDraftMetadataByMemberId(1L, 2L))

        verifyNoInteractions(cards, attachments)
    }

    @Test
    fun `임시저장이 다른 그룹에 속하면 본문 조회는 NotFoundException을 던진다`() {
        val draft = GroupRecordDraft(5L, 2L, 9L, 30L, null, "other", NOW)
        given(drafts.findByMemberId(2L)).willReturn(draft)

        assertThrows<NotFoundException> { service.getGroupRecordDraftByMemberId(1L, 2L) }

        verifyNoInteractions(cards, attachments)
    }

    @Test
    fun `다른 그룹에 속한 임시저장의 삭제를 요청해도 임시저장과 첨부 연결은 유지된다`() {
        val draft = GroupRecordDraft(5L, 2L, 9L, 30L, null, "other", NOW)
        given(drafts.findByMemberId(2L)).willReturn(draft)
        allowGroupEdit()

        service.deleteGroupRecordDraft(1L, 2L)

        verifyNoInteractions(cards, attachments)
        verify(drafts, never()).delete(draft)
    }

    @Test
    fun `임시저장 요청의 저장 시각이 기존 저장 시각보다 이르면 요청을 거부한다`() {
        val draft = existingDraft()
        allowGroupEdit()
        given(drafts.findForUpdateByMemberId(2L)).willReturn(draft)

        assertThrows<ConflictException> {
            service.updateGroupRecordDraft(1L, 2L, 3L, null, "stale", NOW.minusSeconds(1), emptyList())
        }

        assertEquals("before", draft.content)
        assertEquals(NOW, draft.updatedAt)
        verifyNoInteractions(cards, attachments)
    }

    @Test
    fun `임시저장 요청의 저장 시각이 기존 저장 시각과 같아도 본문을 덮어쓴다`() {
        val draft = existingDraft()
        allowGroupEdit()
        given(drafts.findForUpdateByMemberId(2L)).willReturn(draft)
        given(cards.findById(3L)).willReturn(Optional.of(card()))
        given(attachments.replaceDraftFiles(1L, 2L, draft.id, emptyList(), false)).willReturn(emptyList())

        service.updateGroupRecordDraft(1L, 2L, 3L, null, "after", NOW, emptyList())

        assertEquals("after", draft.content)
        assertEquals(NOW, draft.updatedAt)
        verify(attachments).replaceDraftFiles(1L, 2L, draft.id, emptyList(), false)
    }

    @Test
    fun `임시저장 본문은 빈 문자열로 덮어쓸 수 있다`() {
        val draft = existingDraft()
        allowGroupEdit()
        given(drafts.findForUpdateByMemberId(2L)).willReturn(draft)
        given(cards.findById(3L)).willReturn(Optional.of(card()))
        given(attachments.replaceDraftFiles(1L, 2L, draft.id, emptyList(), false)).willReturn(emptyList())

        service.updateGroupRecordDraft(1L, 2L, 3L, null, "", NOW.plusSeconds(1), emptyList())

        assertEquals("", draft.content)
        assertEquals(NOW.plusSeconds(1), draft.updatedAt)
    }

    @Test
    fun `다른 그룹에 저장된 임시저장을 덮어쓸 때 첨부 연결 초기화를 요청한다`() {
        val draft = GroupRecordDraft(5L, 2L, 9L, 30L, null, "before", NOW)
        allowGroupEdit()
        given(groups.findLockedById(9L)).willReturn(testGroup(id = 9L))
        given(drafts.findForUpdateByMemberId(2L)).willReturn(draft)
        given(cards.findById(3L)).willReturn(Optional.of(card()))
        given(attachments.replaceDraftFiles(1L, 2L, draft.id, emptyList(), true))
            .willReturn(emptyList())

        service.updateGroupRecordDraft(1L, 2L, 3L, null, "after", NOW.plusSeconds(1), emptyList())

        assertEquals(1L, draft.groupId)
        assertEquals(3L, draft.cardId)
        assertEquals("after", draft.content)
        assertEquals(NOW.plusSeconds(1), draft.updatedAt)
        verify(attachments).replaceDraftFiles(1L, 2L, draft.id, emptyList(), true)
    }

    @Test
    fun `임시저장 본문이 1 MiB를 넘으면 기존 본문과 첨부 연결을 유지한다`() {
        val draft = existingDraft()
        allowGroupEdit()
        given(drafts.findForUpdateByMemberId(2L)).willReturn(draft)
        given(cards.findById(3L)).willReturn(Optional.of(card()))
        val tooLarge = "\uD83D\uDE00".repeat(1024 * 1024 / 4) + "x"

        assertThrows<BadRequestException> {
            service.updateGroupRecordDraft(1L, 2L, 3L, null, tooLarge, NOW, emptyList())
        }

        assertEquals("before", draft.content)
        verifyNoInteractions(attachments)
    }

    @Test
    fun `다른 회원이 작성한 기록을 수정하는 임시저장은 저장할 수 없다`() {
        val draft = existingDraft()
        allowGroupEdit()
        given(drafts.findForUpdateByMemberId(2L)).willReturn(draft)
        given(cards.findById(3L)).willReturn(Optional.of(card()))
        given(records.findByIdAndDeletedAtIsNull(4L)).willReturn(GroupRecord(4L, 3L, 8L, "original", NOW))

        assertThrows<ForbiddenException> {
            service.updateGroupRecordDraft(1L, 2L, 3L, 4L, "new", NOW, emptyList())
        }

        assertNull(draft.recordId)
        verifyNoInteractions(attachments)
    }

    @Test
    fun `임시저장을 삭제할 때는 첨부 파일 연결을 먼저 해제한다`() {
        val draft = existingDraft()
        allowGroupEdit()
        given(drafts.findByMemberId(2L)).willReturn(draft)

        service.deleteGroupRecordDraft(1L, 2L)

        inOrder(drafts, attachments) {
            verify(drafts).findByMemberId(2L)
            verify(attachments).deleteDraftFiles(draft.id)
            verify(drafts).delete(draft)
        }
    }

    private fun allowGroupEdit() {
        given(members.findForUpdateById(2L)).willReturn(mock())
        given(groups.findLockedById(1L)).willReturn(testGroup())
    }

    private fun existingDraft() = GroupRecordDraft(5L, 2L, 1L, 3L, null, "before", NOW)

    private fun card() = GroupCard(3L, 1L, 6L, 2L, "카드", 1L, NOW)

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
    }
}
