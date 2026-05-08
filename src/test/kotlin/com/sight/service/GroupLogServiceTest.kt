package com.sight.service

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupLogRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.dto.GroupLogListDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime

class GroupLogServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val groupLogRepository = mock<GroupLogRepository>()
    private val groupLogService =
        GroupLogService(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            groupLogRepository = groupLogRepository,
        )

    @Test
    fun `listGroupLogs는 그룹 멤버에게 로그 목록과 전체 count를 반환한다`() {
        // given
        val groupId = 100L
        val requesterId = 1L
        given(groupRepository.existsById(groupId)).willReturn(true)
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)).willReturn(true)
        given(groupLogRepository.findLogsByGroupId(groupId, 0, 100)).willReturn(
            listOf(
                GroupLogListDto(
                    id = 10L,
                    memberId = 1L,
                    message = "참여",
                    createdAt = LocalDateTime.of(2026, 5, 1, 12, 0),
                ),
                GroupLogListDto(
                    id = 9L,
                    memberId = 0L,
                    message = "시스템 로그",
                    createdAt = LocalDateTime.of(2026, 4, 30, 12, 0),
                ),
            ),
        )
        given(groupLogRepository.countLogsByGroupId(groupId)).willReturn(257L)

        // when
        val result = groupLogService.listGroupLogs(groupId = groupId, requesterId = requesterId, offset = 0, limit = 100)

        // then
        assertEquals(257L, result.count)
        assertEquals(2, result.logs.size)
        assertEquals(10L, result.logs[0].id)
        assertEquals(1L, result.logs[0].memberId)
        assertEquals(0L, result.logs[1].memberId)
    }

    @Test
    fun `listGroupLogs는 offset과 limit를 그대로 레포지토리에 전달한다`() {
        // given
        val groupId = 100L
        val requesterId = 1L
        given(groupRepository.existsById(groupId)).willReturn(true)
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)).willReturn(true)
        given(groupLogRepository.findLogsByGroupId(groupId, 50, 20)).willReturn(emptyList())
        given(groupLogRepository.countLogsByGroupId(groupId)).willReturn(0L)

        // when
        groupLogService.listGroupLogs(groupId = groupId, requesterId = requesterId, offset = 50, limit = 20)

        // then
        verify(groupLogRepository).findLogsByGroupId(eq(groupId), eq(50), eq(20))
    }

    @Test
    fun `listGroupLogs는 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.existsById(999L)).willReturn(false)

        // then
        assertThrows<NotFoundException> {
            groupLogService.listGroupLogs(groupId = 999L, requesterId = 1L, offset = 0, limit = 100)
        }
    }

    @Test
    fun `listGroupLogs는 그룹 멤버가 아니면 403을 던진다`() {
        // given
        val groupId = 100L
        val requesterId = 2L
        given(groupRepository.existsById(groupId)).willReturn(true)
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)).willReturn(false)

        // then
        assertThrows<ForbiddenException> {
            groupLogService.listGroupLogs(groupId = groupId, requesterId = requesterId, offset = 0, limit = 100)
        }
    }
}
