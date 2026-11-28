package com.sight.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.core.exception.NotFoundException
import com.sight.domain.expoint.ExpointRule
import com.sight.domain.group.GroupExpointChange
import com.sight.domain.group.GroupLog
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogExpointChange
import com.sight.domain.group.GroupLogType
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.GroupExpointChangeRepository
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
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant

class GroupLogServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupLogRepository = mock<GroupLogRepository>()
    private val groupExpointChangeRepository = mock<GroupExpointChangeRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val integerIdService = mock<IntegerIdService>()
    private val pointService = mock<PointService>()
    private val objectMapper = ObjectMapper()
    private val groupLogService =
        GroupLogService(
            groupRepository = groupRepository,
            groupLogRepository = groupLogRepository,
            groupExpointChangeRepository = groupExpointChangeRepository,
            memberRepository = memberRepository,
            integerIdService = integerIdService,
            objectMapper = objectMapper,
            pointService = pointService,
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

    @Test
    fun `경험치 변동 없이 남긴 로그는 경험치 변동을 저장하지 않는다`() {
        // given
        given(integerIdService.nextId()).willReturn(900L)

        // when
        groupLogService.createLog(groupId = 100L, actorMemberId = 1L, data = GroupLogData.GroupMemberJoined)

        // then
        verifyNoInteractions(groupExpointChangeRepository)
    }

    @Test
    fun `경험치 변동과 함께 남긴 로그는 대상 회원마다 변동 대상과 변동량을 그 로그에 연결해 저장한다`() {
        // given
        given(integerIdService.nextId()).willReturn(900L, 901L, 902L)

        // when
        groupLogService.createLog(
            groupId = 100L,
            actorMemberId = 1L,
            data = GroupLogData.GroupPortfolioPublished,
            expointChange = GroupLogExpointChange(targetId = TARGET_ID, point = 10, memberIds = listOf(1L, 2L)),
        )

        // then
        verify(groupExpointChangeRepository).saveAll(
            argThat<List<GroupExpointChange>> {
                map { listOf(it.groupLogId, it.targetId, it.memberId, it.point) } ==
                    listOf(listOf(900L, TARGET_ID, 1L, 10), listOf(900L, TARGET_ID, 2L, 10))
            },
        )
    }

    @Test
    fun `경험치를 되돌리면 같은 대상에서 지정한 로그타입의 가장 최근 행위로 기록된 회원별 변동량을 반대로 적용하고 되돌린 로그에 연결해 저장한다`() {
        // given
        given(integerIdService.nextId()).willReturn(910L, 911L, 912L)
        given(groupExpointChangeRepository.findLatestGroupLogIdByTargetIdAndLogTypes(TARGET_ID, TARGET_LOG_TYPES)).willReturn(900L)
        given(groupExpointChangeRepository.findByGroupLogId(900L)).willReturn(
            listOf(
                GroupExpointChange(id = 901L, groupLogId = 900L, targetId = TARGET_ID, memberId = 1L, point = 10),
                GroupExpointChange(id = 902L, groupLogId = 900L, targetId = TARGET_ID, memberId = 2L, point = 20),
            ),
        )

        // when
        val applied = revertPortfolio(fallbackMemberIds = listOf(3L))

        // then
        assertEquals(listOf(1L to -10, 2L to -20), applied.map { it.memberId to it.point })
        verify(pointService).givePoint(targetUserId = 1L, point = -10, message = "그룹 포트폴리오 취소")
        verify(pointService).givePoint(targetUserId = 2L, point = -20, message = "그룹 포트폴리오 취소")
        verify(groupLogRepository).save(argThat<GroupLog> { id == 910L && logType == GroupLogType.GROUP_PORTFOLIO_UNPUBLISHED })
        verify(groupExpointChangeRepository).saveAll(
            argThat<List<GroupExpointChange>> { all { it.groupLogId == 910L && it.targetId == TARGET_ID } && size == 2 },
        )
    }

    @Test
    fun `같은 대상에서 지정한 로그타입으로 기록된 경험치 변동이 없으면 현재 규칙의 변동량을 지정한 회원들에게 적용한다`() {
        // given
        given(integerIdService.nextId()).willReturn(910L, 911L, 912L)
        given(groupExpointChangeRepository.findLatestGroupLogIdByTargetIdAndLogTypes(TARGET_ID, TARGET_LOG_TYPES)).willReturn(null)

        // when
        val applied = revertPortfolio(fallbackMemberIds = listOf(3L, 4L))

        // then
        val point = ExpointRule.그룹포트폴리오취소.point
        assertEquals(listOf(3L to point, 4L to point), applied.map { it.memberId to it.point })
        verify(pointService).givePoint(targetUserId = 3L, point = point, message = "그룹 포트폴리오 취소")
        verify(pointService).givePoint(targetUserId = 4L, point = point, message = "그룹 포트폴리오 취소")
    }

    @Test
    fun `같은 대상에서 지정한 로그타입으로 기록된 경험치 변동도 현재 규칙을 적용할 회원도 없으면 경험치를 바꾸지 않고 되돌린 로그만 남긴다`() {
        // given
        given(integerIdService.nextId()).willReturn(910L)
        given(groupExpointChangeRepository.findLatestGroupLogIdByTargetIdAndLogTypes(TARGET_ID, TARGET_LOG_TYPES)).willReturn(null)

        // when
        val applied = revertPortfolio(fallbackMemberIds = emptyList())

        // then
        assertEquals(emptyList<GroupExpointChange>(), applied)
        verifyNoInteractions(pointService)
        verify(groupLogRepository).save(argThat<GroupLog> { logType == GroupLogType.GROUP_PORTFOLIO_UNPUBLISHED })
    }

    private fun revertPortfolio(fallbackMemberIds: List<Long>) =
        groupLogService.revertExpointChange(
            groupId = 100L,
            actorMemberId = 1L,
            targetId = TARGET_ID,
            targetLogTypes = TARGET_LOG_TYPES,
            revertLog = GroupLogData.GroupPortfolioUnpublished,
            message = "그룹 포트폴리오 취소",
            fallbackRule = ExpointRule.그룹포트폴리오취소,
            fallbackMemberIds = fallbackMemberIds,
        )

    companion object {
        private const val TARGET_ID = "100"
        private val TARGET_LOG_TYPES = setOf(GroupLogType.GROUP_PORTFOLIO_PUBLISHED)
    }
}
