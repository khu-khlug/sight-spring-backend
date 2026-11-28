package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.expoint.ExpointRule
import com.sight.domain.group.Group
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogExpointChange
import com.sight.domain.group.GroupLogType
import com.sight.domain.group.GroupMember
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.domain.group.testGroupMember
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import com.sight.repository.dto.GroupMemberListDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.util.Optional

class GroupMemberServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val groupLogService = mock<GroupLogService>()
    private val notificationService = mock<NotificationService>()
    private val pointService = mock<PointService>()
    private val groupMemberService =
        GroupMemberService(
            integerIdService = mock<IntegerIdService>().apply { given(nextId()).willReturn(900000000000000001L) },
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            memberRepository = memberRepository,
            groupLogService = groupLogService,
            notificationService = notificationService,
            pointService = pointService,
        )

    init {
        // 삭제·그룹장 변경이 성공한 상황을 기본으로 둔다. 동시 요청으로 실패하는 경우는 각 테스트에서 따로 정한다.
        given(groupMemberRepository.delete(any(), any())).willReturn(1)
        given(groupRepository.changeLeaderById(any(), any(), any())).willReturn(1)
    }

    private fun createGroup(
        id: Long = 100L,
        master: Long = 1L,
        author: Long = master,
        category: GroupCategory = GroupCategory.STUDY,
        visibility: GroupVisibility = GroupVisibility.MEMBER_ONLY,
        status: GroupStatus = GroupStatus.PROGRESS,
        allowJoin: Boolean = false,
        memberCount: Long = 1L,
    ) = testGroup(
        id = id,
        category = category,
        title = "테스트 그룹",
        leaderMemberId = master,
        authorMemberId = author,
        visibility = visibility,
        status = status,
        allowJoin = allowJoin,
        memberCount = memberCount,
    )

    private fun createMember(
        id: Long = 1L,
        manager: Boolean = false,
        studentStatus: StudentStatus = StudentStatus.UNDERGRADUATE,
    ) = Member(
        id = id,
        name = "user$id",
        realname = "이름$id",
        admission = "19",
        college = "공과대학",
        grade = 3L,
        studentStatus = studentStatus,
        status = UserStatus.ACTIVE,
        manager = manager,
    )

    private fun stubGroupAndRequester(
        group: Group,
        requester: Member,
        isMember: Boolean = true,
    ) {
        given(groupRepository.findById(group.id)).willReturn(Optional.of(group))
        given(groupRepository.findLockedById(group.id)).willReturn(group)
        given(memberRepository.findById(requester.id)).willReturn(Optional.of(requester))
        given(groupMemberRepository.existsByGroupIdAndMemberId(group.id, requester.id)).willReturn(isMember)
    }

    @Test
    fun `listGroupMembers는 그룹장을 맨 앞에 두고 나머지를 실명 오름차순으로 반환한다`() {
        // given
        val group = createGroup(id = 100L, master = 5L, visibility = GroupVisibility.PUBLIC)
        val requester = createMember(id = 1L)
        stubGroupAndRequester(group, requester, isMember = true)
        given(groupMemberRepository.findMemberListByGroupId(100L)).willReturn(
            listOf(
                GroupMemberListDto(userId = 1L, name = "홍길동", college = "공과대학", cardCount = 2L, recordCount = 5L),
                GroupMemberListDto(userId = 5L, name = "김철수", college = "소프트웨어융합대학", cardCount = 3L, recordCount = 1L),
                GroupMemberListDto(userId = 3L, name = "강감찬", college = "전자정보대학", cardCount = 0L, recordCount = 0L),
            ),
        )

        // when
        val result = groupMemberService.listGroupMembers(groupId = 100L)

        // then
        assertEquals(5L, result.leaderUserId)
        assertEquals(3, result.members.size)
        // 그룹장(5L, 김철수) 맨 앞
        assertEquals(5L, result.members[0].userId)
        assertEquals("김철수", result.members[0].name)
        assertEquals("소프트웨어융합대학", result.members[0].college)
        assertEquals(3L, result.members[0].cardCount)
        assertEquals(1L, result.members[0].recordCount)
        // 나머지 실명 오름차순: 강감찬(3L) -> 홍길동(1L)
        assertEquals(3L, result.members[1].userId)
        assertEquals("강감찬", result.members[1].name)
        assertEquals("전자정보대학", result.members[1].college)
        assertEquals(0L, result.members[1].cardCount)
        assertEquals(0L, result.members[1].recordCount)

        assertEquals(1L, result.members[2].userId)
        assertEquals("홍길동", result.members[2].name)
        assertEquals("공과대학", result.members[2].college)
        assertEquals(2L, result.members[2].cardCount)
        assertEquals(5L, result.members[2].recordCount)
    }

    @Test
    fun `listGroupMembers는 그룹장이 없는 경우 leaderUserId가 null이고 전체 멤버를 실명 오름차순으로 반환한다`() {
        // given
        val group = createGroup(id = 100L, master = 1L, visibility = GroupVisibility.PUBLIC)
        // leaderMemberId를 null로 설정
        group.changeLeader(null)
        val requester = createMember(id = 1L)
        stubGroupAndRequester(group, requester, isMember = true)
        given(groupMemberRepository.findMemberListByGroupId(100L)).willReturn(
            listOf(
                GroupMemberListDto(userId = 1L, name = "홍길동", college = "공과대학", cardCount = 1L, recordCount = 2L),
                GroupMemberListDto(userId = 2L, name = "강감찬", college = "전자정보대학", cardCount = 0L, recordCount = 0L),
            ),
        )

        // when
        val result = groupMemberService.listGroupMembers(groupId = 100L)

        // then
        assertEquals(null, result.leaderUserId)
        assertEquals(2, result.members.size)
        assertEquals(2L, result.members[0].userId)
        assertEquals("강감찬", result.members[0].name)
        assertEquals(1L, result.members[1].userId)
        assertEquals("홍길동", result.members[1].name)
    }

    @Test
    fun `listGroupMembers는 멤버가 없는 경우 빈 목록을 반환한다`() {
        // given
        val group = createGroup(id = 100L, master = 5L, visibility = GroupVisibility.PUBLIC)
        val requester = createMember(id = 1L)
        stubGroupAndRequester(group, requester, isMember = true)
        given(groupMemberRepository.findMemberListByGroupId(100L)).willReturn(emptyList())

        // when
        val result = groupMemberService.listGroupMembers(groupId = 100L)

        // then
        assertEquals(5L, result.leaderUserId)
        assertEquals(0, result.members.size)
    }

    @Test
    fun `listGroupMembers는 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findById(999L)).willReturn(Optional.empty())

        // then
        assertThrows<NotFoundException> {
            groupMemberService.listGroupMembers(groupId = 999L)
        }
    }

    @Test
    fun `delegateLeader는 그룹장이 다른 멤버에게 그룹장을 위임할 수 있다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val requester = createMember(id = 1L)
        val newLeader = createMember(id = 2L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(memberRepository.findById(2L)).willReturn(Optional.of(newLeader))
        given(memberRepository.findById(1L)).willReturn(Optional.of(requester))
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 2L)).willReturn(
            GroupMember(id = 2L, groupId = 100L, memberId = 2L, joinedAt = Instant.EPOCH),
        )
        given(groupMemberRepository.findByGroupId(100L)).willReturn(
            listOf(
                testGroupMember(groupId = 100L, memberId = 1L),
                testGroupMember(groupId = 100L, memberId = 2L),
            ),
        )

        // when
        groupMemberService.delegateLeader(groupId = 100L, requesterId = 1L, newLeaderId = 2L)

        // then - 현재 그룹장이 요청자일 때만 그룹장을 바꾼다
        verify(groupRepository).changeLeaderById(100L, 1L, 2L)

        // log 호출: 위임 전후 그룹장 정보를 로그데이터로 남긴다
        verify(groupLogService).createLog(
            100L,
            1L,
            GroupLogData.GroupLeaderDelegated(
                fromUserId = 1L,
                fromUserName = "이름1",
                fromUserCollege = "공과대학",
                toUserId = 2L,
                toUserName = "이름2",
                toUserCollege = "공과대학",
            ),
        )

        // 알림: 멤버 2명 each, content 포맷 검증
        val expectedContent =
            "<a href=\"/group/100#member\"><u>테스트 그룹</u></a> 그룹장이 이름2에게 위임되었습니다."
        verify(notificationService, times(2)).createNotification(
            userId = any(),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = eq(expectedContent),
            url = anyOrNull(),
        )
    }

    @Test
    fun `delegateLeader는 종료된 그룹에서 400을 던진다`() {
        // given
        listOf(GroupStatus.SUCCESS, GroupStatus.FAIL).forEach { status ->
            val group = createGroup(id = 100L, master = 1L, status = status)
            given(groupRepository.findLockedById(100L)).willReturn(group)

            // then
            assertThrows<BadRequestException> {
                groupMemberService.delegateLeader(groupId = 100L, requesterId = 1L, newLeaderId = 2L)
            }
        }
    }

    @Test
    fun `delegateLeader는 위임 대상이 그룹 멤버가 아니면 400을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val requester = createMember(id = 1L)
        val newLeader = createMember(id = 2L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(memberRepository.findById(2L)).willReturn(Optional.of(newLeader))
        given(memberRepository.findById(1L)).willReturn(Optional.of(requester))
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 2L)).willReturn(null)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.delegateLeader(groupId = 100L, requesterId = 1L, newLeaderId = 2L)
        }
        verify(groupRepository, never()).save(any())
    }

    @Test
    fun `delegateLeader는 자기 자신에게 위임하면 400을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val requester = createMember(id = 1L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(memberRepository.findById(1L)).willReturn(Optional.of(requester))

        // then
        assertThrows<BadRequestException> {
            groupMemberService.delegateLeader(groupId = 100L, requesterId = 1L, newLeaderId = 1L)
        }
        verify(groupRepository, never()).save(any())
    }

    @Test
    fun `delegateLeader는 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findLockedById(999L)).willReturn(null)

        // then
        assertThrows<NotFoundException> {
            groupMemberService.delegateLeader(groupId = 999L, requesterId = 1L, newLeaderId = 2L)
        }
    }

    @Test
    fun `delegateLeader는 위임 대상 회원이 존재하지 않으면 404를 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(memberRepository.findById(999L)).willReturn(Optional.empty())

        // then
        assertThrows<NotFoundException> {
            groupMemberService.delegateLeader(groupId = 100L, requesterId = 1L, newLeaderId = 999L)
        }
    }

    @Test
    fun `joinGroup은 일반 카테고리 그룹 참여 시 멤버 추가·로그·알림·포인트 지급·상태 갱신을 수행한다`() {
        // given
        val group =
            createGroup(
                id = 100L,
                master = 5L,
                visibility = GroupVisibility.PUBLIC,
                status = GroupStatus.PROGRESS,
                allowJoin = true,
            )
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = false)

        // when
        groupMemberService.joinGroup(groupId = 100L, requesterId = 2L)

        // then
        verify(groupMemberRepository).save(eq(900000000000000001L), eq(100L), eq(2L), any())
        verify(groupLogService).createLog(
            100L,
            2L,
            GroupLogData.GroupMemberJoined,
            GroupLogExpointChange(
                targetId = "900000000000000001",
                point = ExpointRule.그룹참여.point,
                memberIds = listOf(2L),
            ),
        )

        val expectedContent =
            "<a href=\"/group/100\"><u>테스트 그룹</u></a> 그룹에 참여했습니다."
        verify(notificationService, times(2)).createNotification(
            userId = any(),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = eq(expectedContent),
            url = anyOrNull(),
        )
        // 본인('25')과 그룹장('26') 알림
        verify(notificationService).createNotification(
            userId = eq(2L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = eq(expectedContent),
            url = anyOrNull(),
        )
        verify(notificationService).createNotification(
            userId = eq(5L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = eq(expectedContent),
            url = anyOrNull(),
        )

        verify(pointService).givePoint(
            targetUserId = eq(2L),
            point = eq(ExpointRule.그룹참여.point),
            message = eq("<u>테스트 그룹</u> 그룹에 참여했습니다."),
        )
        verify(groupRepository).addMemberCountById(100L, 1)
    }

    @Test
    fun `joinGroup은 운영 카테고리 그룹이면 전 멤버에게 알림을 발송한다`() {
        // given - 운영 카테고리, 기존 멤버 3명, 참여자 본인 추가
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                category = GroupCategory.MANAGE,
                visibility = GroupVisibility.PUBLIC,
                status = GroupStatus.PROGRESS,
                allowJoin = true,
            )
        val requester = createMember(id = 5L)
        stubGroupAndRequester(group, requester, isMember = false)
        // findByGroupId는 add_member 이후 호출이라 참여자 포함된 결과를 반환해야 함
        given(groupMemberRepository.findByGroupId(100L)).willReturn(
            listOf(
                testGroupMember(groupId = 100L, memberId = 1L),
                testGroupMember(groupId = 100L, memberId = 3L),
                testGroupMember(groupId = 100L, memberId = 5L),
            ),
        )

        // when
        groupMemberService.joinGroup(groupId = 100L, requesterId = 5L, isManager = true)

        // then - 본인 알림 1건 + 전 멤버 알림 3건 (참여자 본인은 add_member 이후라 포함) = 총 4건
        verify(notificationService, times(4)).createNotification(
            userId = any(),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = any(),
            url = anyOrNull(),
        )
        // 본인은 '25'와 '26' 둘 다 받음 = 2회
        verify(notificationService, times(2)).createNotification(
            userId = eq(5L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = any(),
            url = anyOrNull(),
        )
        verify(notificationService).createNotification(
            userId = eq(1L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = any(),
            url = anyOrNull(),
        )
        verify(notificationService).createNotification(
            userId = eq(3L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = any(),
            url = anyOrNull(),
        )
    }

    @Test
    fun `joinGroup은 운영 카테고리 그룹에 비운영진이 참가하려 하면 403을 던진다`() {
        // given
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                category = GroupCategory.MANAGE,
                visibility = GroupVisibility.PUBLIC,
                status = GroupStatus.PROGRESS,
                allowJoin = true,
            )
        val requester = createMember(id = 5L)
        stubGroupAndRequester(group, requester, isMember = false)

        // then
        assertThrows<ForbiddenException> {
            groupMemberService.joinGroup(groupId = 100L, requesterId = 5L, isManager = false)
        }
    }

    @Test
    fun `joinGroup은 SUSPEND 상태 그룹에 참여하면 상태가 PROGRESS로 전환된다`() {
        // given
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                visibility = GroupVisibility.PUBLIC,
                status = GroupStatus.STOP,
                allowJoin = true,
            )
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = false)

        // when (does not throw)
        groupMemberService.joinGroup(groupId = 100L, requesterId = 2L)

        // then
        verify(groupRepository).resumeStoppedById(100L)
    }

    @Test
    fun `joinGroup은 해당 그룹이 참여를 허용하지 않으면 403을 던진다`() {
        // given - 권한 통과 후 allow_join 단계에서 차단
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                visibility = GroupVisibility.PUBLIC,
                status = GroupStatus.PROGRESS,
                allowJoin = false,
            )
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = false)

        // then
        assertThrows<ForbiddenException> {
            groupMemberService.joinGroup(groupId = 100L, requesterId = 2L)
        }
    }

    @Test
    fun `joinGroup은 이미 멤버인 경우 400을 던진다`() {
        // given - 권한·allow_join·state 모두 통과 후 이미 멤버 차단
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                visibility = GroupVisibility.PUBLIC,
                status = GroupStatus.PROGRESS,
                allowJoin = true,
            )
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = true)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.joinGroup(groupId = 100L, requesterId = 2L)
        }
    }

    @Test
    fun `joinGroup은 종료된 그룹에서 400을 던진다`() {
        // given
        listOf(GroupStatus.SUCCESS, GroupStatus.FAIL).forEach { status ->
            val group =
                createGroup(
                    id = 100L,
                    master = 1L,
                    visibility = GroupVisibility.PUBLIC,
                    status = status,
                    allowJoin = true,
                )
            val requester = createMember(id = 2L)
            stubGroupAndRequester(group, requester, isMember = false)

            // then
            assertThrows<BadRequestException> {
                groupMemberService.joinGroup(groupId = 100L, requesterId = 2L)
            }
        }
    }

    @Test
    fun `joinGroup은 중복 참가로 DataIntegrityViolationException이 발생하면 400을 던진다`() {
        // given
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                visibility = GroupVisibility.PUBLIC,
                status = GroupStatus.PROGRESS,
                allowJoin = true,
            )
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = false)
        given(groupMemberRepository.save(any(), eq(100L), eq(2L), any()))
            .willThrow(DataIntegrityViolationException("Duplicate entry"))

        // then
        val ex =
            assertThrows<BadRequestException> {
                groupMemberService.joinGroup(groupId = 100L, requesterId = 2L)
            }
        assertEquals("이미 그룹 멤버입니다", ex.message)
    }

    @Test
    fun `joinGroup은 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findLockedById(999L)).willReturn(null)

        // then
        assertThrows<NotFoundException> {
            groupMemberService.joinGroup(groupId = 999L, requesterId = 1L)
        }
    }

    @Test
    fun `kickMember는 그룹장이 멤버를 내보낼 수 있다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val kickedMember = createMember(id = 5L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(memberRepository.findById(5L)).willReturn(Optional.of(kickedMember))
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 5L)).willReturn(
            GroupMember(id = 777L, groupId = 100L, memberId = 5L, joinedAt = Instant.EPOCH),
        )

        // when
        groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 5L)

        // then
        verify(groupMemberRepository).delete(100L, 5L)
        verify(groupLogService).revertExpointChange(
            groupId = eq(100L),
            actorMemberId = eq(1L),
            targetId = eq("777"),
            targetLogTypes = eq(setOf(GroupLogType.GROUP_MEMBER_JOINED)),
            revertLog = eq(GroupLogData.GroupMemberKicked(targetUserId = 5L, targetUserName = "이름5", targetUserCollege = "공과대학")),
            message = eq("<u>테스트 그룹</u> 그룹에서 내보내졌습니다."),
            fallbackRule = eq(ExpointRule.그룹탈퇴),
            fallbackMemberIds = eq(listOf(5L)),
        )
        verify(notificationService).createNotification(
            userId = eq(5L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = eq("<a href=\"/group/100\"><u>테스트 그룹</u></a> 그룹에서 내보내졌습니다."),
            url = anyOrNull(),
        )
        verify(groupRepository).addMemberCountById(100L, -1)
    }

    @Test
    fun `kickMember는 종료된 그룹에서 400을 던진다`() {
        // given
        listOf(GroupStatus.SUCCESS, GroupStatus.FAIL).forEach { status ->
            val group = createGroup(id = 100L, master = 1L, status = status)
            given(groupRepository.findLockedById(100L)).willReturn(group)

            // then
            assertThrows<BadRequestException> {
                groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 5L)
            }
        }
    }

    @Test
    fun `kickMember는 운영 그룹에서 400을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L, category = GroupCategory.MANAGE)
        given(groupRepository.findLockedById(100L)).willReturn(group)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 5L)
        }
    }

    @Test
    fun `kickMember는 자기 자신을 내보낼 수 없다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        given(groupRepository.findLockedById(100L)).willReturn(group)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 1L)
        }
    }

    @Test
    fun `kickMember는 SUSPEND 상태 그룹에서 내보내면 상태가 PROGRESS로 전환된다`() {
        // given
        val group = createGroup(id = 100L, master = 1L, status = GroupStatus.STOP)
        val kickedMember = createMember(id = 5L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(memberRepository.findById(5L)).willReturn(Optional.of(kickedMember))
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 5L)).willReturn(
            GroupMember(id = 777L, groupId = 100L, memberId = 5L, joinedAt = Instant.EPOCH),
        )

        // when
        groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 5L)

        // then
        verify(groupRepository).resumeStoppedById(100L)
    }

    @Test
    fun `kickMember는 동시에 같은 회원을 내보내 삭제가 이미 끝났으면 400을 던지고 포인트·로그를 남기지 않는다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val kickedMember = createMember(id = 5L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(memberRepository.findById(5L)).willReturn(Optional.of(kickedMember))
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 5L)).willReturn(
            GroupMember(id = 777L, groupId = 100L, memberId = 5L, joinedAt = Instant.EPOCH),
        )
        given(groupMemberRepository.delete(100L, 5L)).willReturn(0)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 5L)
        }
        verify(groupLogService, never()).revertExpointChange(any(), anyOrNull(), any(), any(), any(), any(), any(), any())
        verify(groupRepository, never()).addMemberCountById(any(), any())
    }

    @Test
    fun `kickMember는 대상이 그룹 멤버가 아니면 400을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val kickedMember = createMember(id = 5L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(memberRepository.findById(5L)).willReturn(Optional.of(kickedMember))
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 5L)).willReturn(null)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 5L)
        }
    }

    @Test
    fun `kickMember는 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findLockedById(999L)).willReturn(null)

        // then
        assertThrows<NotFoundException> {
            groupMemberService.kickMember(groupId = 999L, requesterId = 1L, kickedMemberId = 5L)
        }
    }

    @Test
    fun `kickMember는 대상 회원이 존재하지 않으면 404를 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(memberRepository.findById(999L)).willReturn(Optional.empty())

        // then
        assertThrows<NotFoundException> {
            groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 999L)
        }
    }

    @Test
    fun `leaveGroup은 일반 카테고리 그룹 탈퇴 시 멤버 삭제·로그·알림·포인트 차감·상태 갱신을 수행한다`() {
        // given - 일반 카테고리, requester는 일반 멤버 (master 아님)
        val group = createGroup(id = 100L, master = 1L)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 5L)).willReturn(
            GroupMember(id = 888L, groupId = 100L, memberId = 5L, joinedAt = Instant.EPOCH),
        )

        // when
        groupMemberService.leaveGroup(groupId = 100L, requesterId = 5L)

        // then
        verify(groupMemberRepository).delete(100L, 5L)
        verify(groupLogService).revertExpointChange(
            groupId = eq(100L),
            actorMemberId = eq(5L),
            targetId = eq("888"),
            targetLogTypes = eq(setOf(GroupLogType.GROUP_MEMBER_JOINED)),
            revertLog = eq(GroupLogData.GroupMemberLeft),
            message = eq("<u>테스트 그룹</u> 그룹에서 나갔습니다."),
            fallbackRule = eq(ExpointRule.그룹탈퇴),
            fallbackMemberIds = eq(listOf(5L)),
        )

        val expectedContent = "<a href=\"/group/100\"><u>테스트 그룹</u></a> 그룹에서 나갔습니다."
        verify(notificationService, times(2)).createNotification(
            userId = any(),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = eq(expectedContent),
            url = anyOrNull(),
        )
        // 본인 + 그룹장
        verify(notificationService).createNotification(
            userId = eq(5L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = eq(expectedContent),
            url = anyOrNull(),
        )
        verify(notificationService).createNotification(
            userId = eq(1L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = eq(expectedContent),
            url = anyOrNull(),
        )

        verify(groupRepository).addMemberCountById(100L, -1)
    }

    @Test
    fun `leaveGroup은 종료된 그룹에서 400을 던진다`() {
        // given
        listOf(GroupStatus.SUCCESS, GroupStatus.FAIL).forEach { status ->
            val group = createGroup(id = 100L, master = 1L, status = status)
            given(groupRepository.findLockedById(100L)).willReturn(group)

            // then
            assertThrows<BadRequestException> {
                groupMemberService.leaveGroup(groupId = 100L, requesterId = 5L)
            }
        }
    }

    @Test
    fun `leaveGroup은 운영 카테고리에서 잔여 멤버 전체에게 알림을 발송하고 남은 멤버 중 한 명을 그룹장으로 지정한다`() {
        // given - 운영 카테고리, master(1L)가 탈퇴, 잔여 멤버 2L, 3L
        val group = createGroup(id = 100L, master = 1L, category = GroupCategory.MANAGE)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 1L)).willReturn(
            GroupMember(id = 888L, groupId = 100L, memberId = 1L, joinedAt = Instant.EPOCH),
        )
        // leave_member 이후의 멤버 목록 (탈퇴자 본인은 빠짐)
        given(groupMemberRepository.findByGroupId(100L)).willReturn(
            listOf(
                testGroupMember(groupId = 100L, memberId = 2L),
                testGroupMember(groupId = 100L, memberId = 3L),
            ),
        )

        // when
        groupMemberService.leaveGroup(groupId = 100L, requesterId = 1L)

        // then - 남은 멤버 2L을 새 그룹장으로 지정
        verify(groupRepository).changeLeaderById(100L, 1L, 2L)

        // 본인 1건 + 잔여 멤버 2건 = 3건
        verify(notificationService, times(3)).createNotification(
            userId = any(),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = any(),
            url = anyOrNull(),
        )
        verify(notificationService).createNotification(
            userId = eq(1L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = any(),
            url = anyOrNull(),
        )
        verify(notificationService).createNotification(
            userId = eq(2L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = any(),
            url = anyOrNull(),
        )
        verify(notificationService).createNotification(
            userId = eq(3L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = any(),
            url = anyOrNull(),
        )
    }

    @Test
    fun `leaveGroup은 운영 카테고리에서 마지막 그룹원인 그룹장이 탈퇴하면 leaderMemberId를 null로 저장한다`() {
        // given - 운영 카테고리, 마지막 멤버 1L 탈퇴
        val group = createGroup(id = 100L, master = 1L, category = GroupCategory.MANAGE)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 1L)).willReturn(
            GroupMember(id = 888L, groupId = 100L, memberId = 1L, joinedAt = Instant.EPOCH),
        )
        given(groupMemberRepository.findByGroupId(100L)).willReturn(emptyList())

        // when
        groupMemberService.leaveGroup(groupId = 100L, requesterId = 1L)

        // then
        verify(groupRepository).clearLeaderById(100L)
    }

    @Test
    fun `leaveGroup은 SUSPEND 상태 그룹에서 탈퇴하면 상태가 PROGRESS로 전환된다`() {
        // given
        val group = createGroup(id = 100L, master = 1L, status = GroupStatus.STOP)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(groupMemberRepository.findLockedByGroupIdAndMemberId(100L, 5L)).willReturn(
            GroupMember(id = 888L, groupId = 100L, memberId = 5L, joinedAt = Instant.EPOCH),
        )

        // when
        groupMemberService.leaveGroup(groupId = 100L, requesterId = 5L)

        // then
        verify(groupRepository).resumeStoppedById(100L)
    }

    @Test
    fun `leaveGroup은 일반 카테고리에서 그룹장이 탈퇴하려 하면 400을 던진다`() {
        // given - 일반 카테고리, requester == master
        val group = createGroup(id = 100L, master = 1L, category = GroupCategory.STUDY)
        given(groupRepository.findLockedById(100L)).willReturn(group)
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 1L)).willReturn(true)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.leaveGroup(groupId = 100L, requesterId = 1L)
        }
    }

    @Test
    fun `leaveGroup은 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findLockedById(999L)).willReturn(null)

        // then
        assertThrows<NotFoundException> {
            groupMemberService.leaveGroup(groupId = 999L, requesterId = 1L)
        }
    }
}
