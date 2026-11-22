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
    fun `다른 그룹의 임시저장은 요약에는 없고 본문 조회와 삭제에서 노출되지 않는다`() {
        val draft = GroupRecordDraft(5L, 2L, 9L, 30L, null, "other", NOW)
        given(drafts.findByMemberId(2L)).willReturn(draft)
        allowGroupEdit()

        assertNull(service.getGroupRecordDraftMetadataByMemberId(1L, 2L))
        assertThrows<NotFoundException> { service.getGroupRecordDraftByMemberId(1L, 2L) }
        service.deleteGroupRecordDraft(1L, 2L)

        verifyNoInteractions(cards, attachments)
        verify(drafts, never()).delete(draft)
    }

    @Test
    fun `서버보다 오래된 저장 요청은 기존 본문과 첨부를 보존한다`() {
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
    fun `같은 시각에는 저장할 수 있고 그룹을 바꾸면 이전 첨부 연결을 초기화한다`() {
        val draft = GroupRecordDraft(5L, 2L, 9L, 30L, null, "before", NOW)
        allowGroupEdit()
        given(groups.findLockedById(9L)).willReturn(testGroup(id = 9L))
        given(drafts.findForUpdateByMemberId(2L)).willReturn(draft)
        given(cards.findById(3L)).willReturn(Optional.of(card()))
        given(attachments.replaceDraftFiles(1L, 2L, draft.id, emptyList(), true))
            .willReturn(RecordFilesChangeResult(emptyList(), true))

        service.updateGroupRecordDraft(1L, 2L, 3L, null, "", NOW, emptyList())

        assertEquals(1L, draft.groupId)
        assertEquals(3L, draft.cardId)
        assertEquals("", draft.content)
        verify(attachments).replaceDraftFiles(1L, 2L, draft.id, emptyList(), true)
    }

    @Test
    fun `같은 시각이어도 내용이 한 바이트 초과하면 기존 임시저장을 바꾸지 않는다`() {
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
    fun `다른 작성자의 기록 수정은 임시저장할 수 없다`() {
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
    fun `본인 임시저장을 삭제하면 첨부 연결을 먼저 해제한다`() {
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
