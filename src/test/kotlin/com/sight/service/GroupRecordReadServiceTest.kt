package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupRecordArchive
import com.sight.domain.group.GroupRecordFile
import com.sight.domain.member.Member
import com.sight.repository.GroupCardRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRecordArchiveRepository
import com.sight.repository.GroupRecordFileRepository
import com.sight.repository.GroupRecordRepository
import com.sight.repository.MemberRepository
import com.sight.repository.dto.GroupRecordFileView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant
import java.util.Optional

class GroupRecordReadServiceTest {
    private val records = mock<GroupRecordRepository>()
    private val cards = mock<GroupCardRepository>()
    private val groupMembers = mock<GroupMemberRepository>()
    private val members = mock<MemberRepository>()
    private val archives = mock<GroupRecordArchiveRepository>()
    private val recordFileRepository = mock<GroupRecordFileRepository>()
    private val service = GroupRecordReadService(records, cards, mock(), members, archives, recordFileRepository, groupMembers)

    @Test
    fun `삭제한 기록은 같은 그룹에서만 삭제 상태로 응답한다`() {
        given(records.existsDeletedByIdAndGroupId(4L, 1L)).willReturn(true)

        assertThrows<BadRequestException> { service.getGroupRecordById(1L, 4L, 2L) }
        assertThrows<NotFoundException> { service.getGroupRecordById(9L, 4L, 2L) }

        verifyNoInteractions(cards)
    }

    @Test
    fun `삭제된 기록을 함께 조회하려면 열람 권한 외에 그룹원 자격이 필요하다`() {
        given(cards.findById(3L)).willReturn(Optional.of(card()))

        assertThrows<ForbiddenException> { service.listGroupRecords(1L, 3L, 2L, 0L, 10, true) }

        verify(groupMembers).existsByGroupIdAndMemberId(1L, 2L)
        verifyNoInteractions(records)
    }

    @Test
    fun `끝 위치부터 요청하면 페이지를 읽지 않고 전체 개수와 빈 목록을 반환한다`() {
        given(cards.findById(3L)).willReturn(Optional.of(card()))
        given(records.countByCardId(3L, false)).willReturn(10L)

        assertEquals(GroupRecordListResult(10L, 10L, emptyList()), service.listGroupRecords(1L, 3L, 2L, 10L, 10, false))
        verify(records, never()).findByCardIdOrderByRecordSort(any(), any(), any(), any(), any())
    }

    @Test
    fun `목록은 정렬 설정을 전달하고 각 기록에 해당하는 첨부와 요청자의 보관 여부를 연결한다`() {
        val card = card()
        val first = GroupRecord(5L, card.id, 8L, "first", NOW)
        val second = GroupRecord(4L, card.id, 8L, "second", NOW)
        val recordIds = listOf(first.id, second.id)
        val author = mock<Member>()
        given(author.id).willReturn(8L)
        given(author.realname).willReturn("작성자")
        given(cards.findById(card.id)).willReturn(Optional.of(card))
        given(records.countByCardId(card.id, false)).willReturn(3L)
        given(records.findByCardIdOrderByRecordSort(card.id, false, card.recordSort, 1L, 2)).willReturn(listOf(first, second))
        given(members.findAllById(listOf(8L))).willReturn(listOf(author))
        given(archives.findByMemberIdAndRecordIdIn(2L, recordIds)).willReturn(listOf(GroupRecordArchive(9L, 2L, 4L, 1L, NOW)))
        val refType = FileReferenceType.GROUP_RECORD_ATTACHMENT
        val firstFile = FileUpload("first-file", "key-1", 8L, GroupFilePaths.recordAttachmentApiPath(1L), "first.txt", 1L, "text/plain", NOW)
        val secondFile = FileUpload("second-file", "key-2", 8L, GroupFilePaths.recordAttachmentApiPath(1L), "second.txt", 2L, "text/plain", NOW)
        secondFile.markUploaded()
        given(recordFileRepository.findByRefTypeAndReferrerInOrderById(refType, listOf("5", "4"))).willReturn(
            listOf(
                GroupRecordFileView(GroupRecordFile(11L, "ref-2", "block-2"), FileReference("ref-2", secondFile.id, refType, "4"), secondFile),
                GroupRecordFileView(GroupRecordFile(12L, "ref-1", "block-1"), FileReference("ref-1", firstFile.id, refType, "5"), firstFile),
            ),
        )

        val result = service.listGroupRecords(1L, card.id, 2L, 1L, 2, false)

        assertEquals(recordIds, result.records.map { it.id })
        assertEquals(listOf(false, true), result.records.map { it.isInUserArchive })
        assertEquals(listOf(firstFile.id, secondFile.id), result.records.map { it.files.single().fileId })
        assertEquals(listOf(null, "/groups/1/files/second-file"), result.records.map { it.files.single().url })
    }

    private fun card() = GroupCard(3L, 1L, 5L, 2L, "카드", 1L, NOW)

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
    }
}
