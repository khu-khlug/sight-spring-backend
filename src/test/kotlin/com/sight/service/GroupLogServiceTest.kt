package com.sight.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupLog
import com.sight.domain.group.GroupLogData
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
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Instant

class GroupLogServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupLogRepository = mock<GroupLogRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val integerIdService = mock<IntegerIdService>()
    private val objectMapper = ObjectMapper()
    private val groupLogService =
        GroupLogService(
            groupRepository = groupRepository,
            groupLogRepository = groupLogRepository,
            memberRepository = memberRepository,
            integerIdService = integerIdService,
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
    fun `listGroupLogs는 행위자와 로그데이터를 담은 로그 목록과 전체 개수를 반환한다`() {
        // given
        val groupId = 100L
        given(groupRepository.existsById(groupId)).willReturn(true)
        given(groupRepository.findGroupLogsByGroupIdOrderByLatest(groupId, 0L, GroupLogService.PAGE_SIZE)).willReturn(
            listOf(
                GroupLog(
                    id = 2L,
                    groupId = groupId,
                    actorMemberId = 1L,
                    logType = GroupLogType.GROUP_MEMBER_JOINED,
                    logData = "{}",
                    createdAt = Instant.parse("2026-05-01T03:00:00Z"),
                ),
                GroupLog(
                    id = 1L,
                    groupId = groupId,
                    actorMemberId = null,
                    logType = GroupLogType.LEGACY,
                    logData = """{"html":"<b>시스템 로그</b>"}""",
                    createdAt = Instant.parse("2026-04-30T03:00:00Z"),
                ),
            ),
        )
        given(groupRepository.countGroupLogsByGroupId(groupId)).willReturn(257L)
        given(memberRepository.findAllById(listOf(1L))).willReturn(listOf(createMember(1L)))

        // when
        val result = groupLogService.listGroupLogs(groupId = groupId, page = 0)

        // then
        assertEquals(0, result.page)
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
    fun `listGroupLogs는 요청한 페이지의 로그 50개를 조회한다`() {
        // given
        val groupId = 100L
        given(groupRepository.existsById(groupId)).willReturn(true)
        given(groupRepository.findGroupLogsByGroupIdOrderByLatest(groupId, 100L, 50)).willReturn(emptyList())
        given(groupRepository.countGroupLogsByGroupId(groupId)).willReturn(0L)

        // when
        groupLogService.listGroupLogs(groupId = groupId, page = 2)

        // then
        verify(groupRepository).findGroupLogsByGroupIdOrderByLatest(eq(groupId), eq(100L), eq(50))
    }

    @Test
    fun `listGroupLogs는 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.existsById(999L)).willReturn(false)

        // then
        assertThrows<NotFoundException> {
            groupLogService.listGroupLogs(groupId = 999L, page = 0)
        }
    }

    @Test
    fun `회원이 남긴 로그는 발급한 아이디와 행위자, 이벤트 로그데이터로 저장한다`() {
        // given
        given(integerIdService.nextId()).willReturn(900L)

        // when
        groupLogService.createLog(
            groupId = 100L,
            actorMemberId = 1L,
            data =
                GroupLogData.CardMoved(
                    cardId = 2001L,
                    cardTitle = "API 설계",
                    fromListId = 1001L,
                    fromListTitle = "할 일",
                    toListId = 1002L,
                    toListTitle = "진행 중",
                ),
        )

        // then
        verify(groupLogRepository).save(
            argThat<GroupLog> {
                val logData = objectMapper.readTree(logData)
                id == 900L &&
                    groupId == 100L &&
                    actorMemberId == 1L &&
                    logType == GroupLogType.CARD_MOVED &&
                    logData["cardId"].asText() == "2001" &&
                    logData["fromListId"].isTextual &&
                    logData["toListTitle"].asText() == "진행 중"
            },
        )
    }

    @Test
    fun `로그데이터의 회원 식별자는 숫자로 저장한다`() {
        // given
        given(integerIdService.nextId()).willReturn(900L)

        // when
        groupLogService.createLog(
            groupId = 100L,
            actorMemberId = 1L,
            data = GroupLogData.GroupMemberKicked(targetUserId = 2L, targetUserName = "이름2", targetUserCollege = "공과대학"),
        )

        // then
        verify(groupLogRepository).save(
            argThat<GroupLog> { objectMapper.readTree(logData)["targetUserId"].isNumber },
        )
    }

    @Test
    fun `시스템이 남긴 로그는 행위자 없이 저장한다`() {
        // given
        given(integerIdService.nextId()).willReturn(900L)

        // when
        groupLogService.createSystemLog(groupId = 100L, data = GroupLogData.GroupStopped)

        // then
        verify(groupLogRepository).save(
            argThat<GroupLog> {
                actorMemberId == null && logType == GroupLogType.GROUP_STOPPED && logData == "{}"
            },
        )
    }
}
