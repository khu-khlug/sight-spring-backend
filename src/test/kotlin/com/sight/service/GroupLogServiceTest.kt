package com.sight.service

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.dto.GroupLogListDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.willDoNothing
import org.mockito.BDDMockito.willThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

class GroupLogServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val groupLogService =
        GroupLogService(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
        )

    @Test
    fun `listGroupLogs는 그룹 멤버에게 로그 목록과 전체 count를 반환한다`() {
        // given
        val groupId = 100L
        val requesterId = 1L
        given(groupRepository.existsById(groupId)).willReturn(true)
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)).willReturn(true)
        given(groupRepository.findGroupLogsByGroupId(groupId, 0, 100)).willReturn(
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
        given(groupRepository.countGroupLogsByGroupId(groupId)).willReturn(257L)

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
        given(groupRepository.findGroupLogsByGroupId(groupId, 50, 20)).willReturn(emptyList())
        given(groupRepository.countGroupLogsByGroupId(groupId)).willReturn(0L)

        // when
        groupLogService.listGroupLogs(groupId = groupId, requesterId = requesterId, offset = 50, limit = 20)

        // then
        verify(groupRepository).findGroupLogsByGroupId(eq(groupId), eq(50), eq(20))
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

    @Test
    fun `createLog는 채번한 ID로 한 번 insert한다`() {
        // given
        willDoNothing().given(groupRepository).insertGroupLog(any(), any(), any(), any())

        // when
        groupLogService.createLog(groupId = 100L, memberId = 1L, message = "테스트 메시지")

        // then
        verify(groupRepository).insertGroupLog(any(), eq(100L), eq(1L), eq("테스트 메시지"))
    }

    @Test
    fun `createLog는 ID 충돌 시 재시도하여 성공한다`() {
        // given - 2번 실패 후 3번째 성공
        willThrow(DataIntegrityViolationException("collision1"))
            .willThrow(DataIntegrityViolationException("collision2"))
            .willDoNothing()
            .given(groupRepository).insertGroupLog(any(), any(), any(), any())

        // when
        groupLogService.createLog(groupId = 100L, memberId = 1L, message = "테스트")

        // then - 총 3번 호출됨
        verify(groupRepository, times(3)).insertGroupLog(any(), any(), any(), any())
    }

    @Test
    fun `createLog는 4회 연속 충돌 시 IllegalStateException을 던진다`() {
        // given - 항상 충돌 (4회 모두 실패)
        willThrow(DataIntegrityViolationException("collision"))
            .given(groupRepository).insertGroupLog(any(), any(), any(), any())

        // then
        assertThrows<IllegalStateException> {
            groupLogService.createLog(groupId = 100L, memberId = 1L, message = "테스트")
        }
        verify(groupRepository, times(4)).insertGroupLog(any(), any(), any(), any())
    }
}
