package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupCard
import com.sight.domain.group.GroupRecord
import com.sight.domain.group.GroupRecordArchive
import com.sight.domain.group.testGroup
import com.sight.repository.GroupRecordArchiveRepository
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

class GroupRecordArchiveServiceTest {
    private val groupRecordArchiveRepository = mock<GroupRecordArchiveRepository>()
    private val support = mock<GroupRecordMutationSupport>()
    private val notifications = mock<NotificationService>()
    private val groupRecordArchiveService =
        GroupRecordArchiveService(groupRecordArchiveRepository, support, notifications, mock())

    @Test
    fun `page만큼 10개씩 건너뛴 위치부터 요청자의 보관 기록을 조회한다`() {
        given(groupRecordArchiveRepository.findSavedRecordsByMemberIdOrderBySavedAtDesc(MEMBER_ID, 10L, 10))
            .willReturn(emptyList())
        given(groupRecordArchiveRepository.countByMemberId(MEMBER_ID)).willReturn(11L)

        val result = groupRecordArchiveService.listSavedRecords(MEMBER_ID, 1)

        verify(groupRecordArchiveRepository).findSavedRecordsByMemberIdOrderBySavedAtDesc(MEMBER_ID, 10L, 10)
        assertEquals(1, result.page)
        assertEquals(11L, result.totalCount)
        assertEquals(0, result.receivedCount)
    }

    @Test
    fun `이미 보관한 기록을 다시 보관해도 행이나 알림이 추가되지 않는다`() {
        val context = context()
        given(support.getLockedRecordContext(1L, MEMBER_ID, 4L, allowStopped = false)).willReturn(context)
        given(groupRecordArchiveRepository.findByMemberIdAndRecordId(MEMBER_ID, 4L))
            .willReturn(GroupRecordArchive(7L, MEMBER_ID, 4L, 1L, NOW))

        groupRecordArchiveService.updateGroupRecordArchive(1L, MEMBER_ID, 4L, true)

        verify(groupRecordArchiveRepository, never()).save(any<GroupRecordArchive>())
        verifyNoInteractions(notifications)
    }

    @Test
    fun `삭제된 원본은 새로 보관할 수 없지만 기존 보관은 해제할 수 있다`() {
        val context = context()
        context.record.delete(NOW)
        given(support.getLockedRecordContext(1L, MEMBER_ID, 4L, allowStopped = false)).willReturn(context)
        val archive = GroupRecordArchive(7L, MEMBER_ID, 4L, 1L, NOW)
        given(groupRecordArchiveRepository.findByMemberIdAndRecordId(MEMBER_ID, 4L)).willReturn(archive)

        assertThrows<BadRequestException> {
            groupRecordArchiveService.updateGroupRecordArchive(1L, MEMBER_ID, 4L, true)
        }
        verifyNoInteractions(groupRecordArchiveRepository, notifications)

        groupRecordArchiveService.updateGroupRecordArchive(1L, MEMBER_ID, 4L, false)

        verify(groupRecordArchiveRepository).delete(archive)
        verify(groupRecordArchiveRepository, never()).save(any<GroupRecordArchive>())
    }

    private fun context(): GroupRecordMutationContext =
        GroupRecordMutationContext(
            testGroup(),
            GroupCard(3L, 1L, 6L, 8L, "카드", 1L, NOW),
            GroupRecord(4L, 3L, 8L, "본문", NOW),
        )

    companion object {
        private const val MEMBER_ID = 2L
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
    }
}
