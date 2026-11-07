package com.sight.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupLog
import com.sight.domain.group.GroupLogType
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.GroupLogRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.Optional

class GroupLogServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupLogRepository = mock<GroupLogRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val objectMapper = ObjectMapper()
    private val groupLogService =
        GroupLogService(
            groupRepository = groupRepository,
            groupLogRepository = groupLogRepository,
            memberRepository = memberRepository,
            objectMapper = objectMapper,
        )

    private fun createMember(id: Long) =
        Member(
            id = id,
            name = "user$id",
            realname = "이름$id",
            admission = "19",
            college = "공과대학",
            grade = 3L,
            studentStatus = StudentStatus.UNDERGRADUATE,
            status = UserStatus.ACTIVE,
        )

    @Test
    fun `listGroupLogs는 그룹 멤버에게 로그 목록과 전체 개수를 반환한다`() {
        // given
        val groupId = 100L
        given(groupRepository.existsById(groupId)).willReturn(true)
        given(groupRepository.findGroupLogsByGroupIdOrderByLatest(groupId, 0, GroupLogService.PAGE_SIZE)).willReturn(
            listOf(
                GroupLog(
                    groupId = groupId,
                    actorMemberId = 1L,
                    actorName = "이름1",
                    logType = GroupLogType.GROUP_MEMBER_JOINED,
                    logData = "{}",
                    createdAt = Instant.parse("2026-05-01T03:00:00Z"),
                ),
                GroupLog(
                    groupId = groupId,
                    actorMemberId = null,
                    actorName = null,
                    logType = GroupLogType.LEGACY,
                    logData = """{"html":"<b>시스템 로그</b>"}""",
                    createdAt = Instant.parse("2026-04-30T03:00:00Z"),
                ),
            ),
        )
        given(groupRepository.countGroupLogsByGroupId(groupId)).willReturn(257L)

        // when
        val result = groupLogService.listGroupLogs(groupId = groupId, skip = 0)

        // then
        assertEquals(0, result.skip)
        assertEquals(257L, result.totalCount)
        assertEquals(2, result.receivedCount)
        assertEquals(1L, result.logs[0].actor?.userId)
        assertEquals("이름1", result.logs[0].actor?.name)
        assertEquals(GroupLogType.GROUP_MEMBER_JOINED, result.logs[0].logType)
        assertEquals(emptyMap<String, Any?>(), result.logs[0].logData)
        assertNull(result.logs[1].actor)
        assertEquals(mapOf("html" to "<b>시스템 로그</b>"), result.logs[1].logData)
    }

    @Test
    fun `listGroupLogs는 skip을 offset으로, 한 번에 최대 50개를 레포지토리에 요청한다`() {
        // given
        val groupId = 100L
        given(groupRepository.existsById(groupId)).willReturn(true)
        given(groupRepository.findGroupLogsByGroupIdOrderByLatest(groupId, 50, 50)).willReturn(emptyList())
        given(groupRepository.countGroupLogsByGroupId(groupId)).willReturn(0L)

        // when
        groupLogService.listGroupLogs(groupId = groupId, skip = 50)

        // then
        verify(groupRepository).findGroupLogsByGroupIdOrderByLatest(eq(groupId), eq(50), eq(50))
    }

    @Test
    fun `listGroupLogs는 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.existsById(999L)).willReturn(false)

        // then
        assertThrows<NotFoundException> {
            groupLogService.listGroupLogs(groupId = 999L, skip = 0)
        }
    }

    @Test
    fun `createLog는 행위자 실명과 로그데이터 JSON을 함께 저장한다`() {
        // given
        given(memberRepository.findById(1L)).willReturn(Optional.of(createMember(1L)))

        // when
        groupLogService.createLog(
            groupId = 100L,
            actorMemberId = 1L,
            logType = GroupLogType.GROUP_MEMBER_KICKED,
            logData = mapOf("targetUserId" to 2L, "targetUserName" to "이름2", "targetUserCollege" to "공과대학"),
        )

        // then
        verify(groupLogRepository).save(
            argThat<GroupLog> {
                groupId == 100L &&
                    actorMemberId == 1L &&
                    actorName == "이름1" &&
                    logType == GroupLogType.GROUP_MEMBER_KICKED &&
                    objectMapper.readTree(logData)["targetUserId"].asLong() == 2L
            },
        )
    }

    @Test
    fun `createLog는 로그타입의 필수 필드가 없으면 저장하지 않는다`() {
        // then
        assertThrows<IllegalArgumentException> {
            groupLogService.createLog(
                groupId = 100L,
                actorMemberId = 1L,
                logType = GroupLogType.GROUP_MEMBER_KICKED,
                logData = mapOf("targetUserId" to 2L),
            )
        }
        verify(groupLogRepository, never()).save(any())
    }

    @Test
    fun `createLog는 신규 로그를 LEGACY로 저장하지 않는다`() {
        // then
        assertThrows<IllegalArgumentException> {
            groupLogService.createLog(
                groupId = 100L,
                actorMemberId = 1L,
                logType = GroupLogType.LEGACY,
                logData = mapOf("html" to "<b>로그</b>"),
            )
        }
        verify(groupLogRepository, never()).save(any())
    }
}
