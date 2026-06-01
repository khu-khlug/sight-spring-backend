package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupMember
import com.sight.domain.group.GroupPolicy
import com.sight.domain.group.GroupState
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
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.LocalDateTime
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
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            memberRepository = memberRepository,
            groupLogService = groupLogService,
            notificationService = notificationService,
            pointService = pointService,
        )

    private fun createGroup(
        id: Long = 100L,
        master: Long = 1L,
        author: Long = master,
        category: GroupCategory = GroupCategory.STUDY,
        grade: GroupAccessGrade = GroupAccessGrade.MEMBER,
        state: GroupState = GroupState.PENDING,
        allowJoin: Boolean = false,
        changedAt: LocalDateTime = LocalDateTime.now(),
    ) = Group(
        id = id,
        category = category,
        title = "테스트 그룹",
        author = author,
        master = master,
        grade = grade,
        state = state,
        allowJoin = allowJoin,
        changedAt = changedAt,
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
        given(memberRepository.findById(requester.id)).willReturn(Optional.of(requester))
        given(groupMemberRepository.existsByGroupIdAndMemberId(group.id, requester.id)).willReturn(isMember)
    }

    @Test
    fun `listGroupMembers는 그룹장을 맨 앞에 두고 나머지를 userId 오름차순으로 반환한다`() {
        // given
        val group = createGroup(id = 100L, master = 5L, grade = GroupAccessGrade.ALL)
        val requester = createMember(id = 1L)
        stubGroupAndRequester(group, requester, isMember = true)
        given(groupMemberRepository.findMemberListByGroupId(100L)).willReturn(
            listOf(
                GroupMemberListDto(userId = 1L, name = "user1", realname = "이름1"),
                GroupMemberListDto(userId = 5L, name = "user5", realname = "이름5"),
                GroupMemberListDto(userId = 3L, name = "user3", realname = "이름3"),
            ),
        )

        // when
        val result = groupMemberService.listGroupMembers(groupId = 100L, requesterId = 1L)

        // then
        assertEquals(3, result.members.size)
        assertEquals(5L, result.members[0].userId)
        assertEquals(true, result.members[0].isLeader)
        assertEquals(1L, result.members[1].userId)
        assertEquals(false, result.members[1].isLeader)
        assertEquals(3L, result.members[2].userId)
        assertEquals(false, result.members[2].isLeader)
    }

    @Test
    fun `listGroupMembers는 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findById(999L)).willReturn(Optional.empty())

        // then
        assertThrows<NotFoundException> {
            groupMemberService.listGroupMembers(groupId = 999L, requesterId = 1L)
        }
    }

    @Test
    fun `PRIVATE 그룹은 멤버만 조회 가능하다`() {
        // given
        val group = createGroup(grade = GroupAccessGrade.PRIVATE)
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = true)
        given(groupMemberRepository.findMemberListByGroupId(group.id)).willReturn(emptyList())

        // when (does not throw)
        groupMemberService.listGroupMembers(groupId = group.id, requesterId = requester.id)
    }

    @Test
    fun `PRIVATE 그룹은 비멤버이고 author도 아니면 403을 던진다`() {
        // given
        val group = createGroup(grade = GroupAccessGrade.PRIVATE, master = 1L, author = 1L)
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = false)

        // then
        assertThrows<ForbiddenException> {
            groupMemberService.listGroupMembers(groupId = group.id, requesterId = requester.id)
        }
    }

    @Test
    fun `MANAGER 그룹은 운영진만 조회 가능하다`() {
        // given
        val group = createGroup(grade = GroupAccessGrade.MANAGER)
        val requester = createMember(id = 2L, manager = true)
        stubGroupAndRequester(group, requester, isMember = false)
        given(groupMemberRepository.findMemberListByGroupId(group.id)).willReturn(emptyList())

        // when (does not throw)
        groupMemberService.listGroupMembers(groupId = group.id, requesterId = requester.id)
    }

    @Test
    fun `MANAGER 그룹은 운영진이 아닌 회원이 조회하면 403을 던진다`() {
        // given
        val group = createGroup(grade = GroupAccessGrade.MANAGER)
        val requester = createMember(id = 2L, manager = false)
        stubGroupAndRequester(group, requester, isMember = true)

        // then
        assertThrows<ForbiddenException> {
            groupMemberService.listGroupMembers(groupId = group.id, requesterId = requester.id)
        }
    }

    @Test
    fun `MEMBER 그룹은 UNDERGRADUATE·ABSENCE·GRADUATE 회원이 조회 가능하다`() {
        // given
        val group = createGroup(grade = GroupAccessGrade.MEMBER)
        listOf(StudentStatus.UNDERGRADUATE, StudentStatus.ABSENCE, StudentStatus.GRADUATE).forEach { status ->
            val requester = createMember(id = 2L, studentStatus = status)
            stubGroupAndRequester(group, requester, isMember = false)
            given(groupMemberRepository.findMemberListByGroupId(group.id)).willReturn(emptyList())

            // when (does not throw)
            groupMemberService.listGroupMembers(groupId = group.id, requesterId = requester.id)
        }
    }

    @Test
    fun `MEMBER 그룹은 UNITED 회원이 조회하면 403을 던진다`() {
        // given
        val group = createGroup(grade = GroupAccessGrade.MEMBER)
        val requester = createMember(id = 2L, studentStatus = StudentStatus.UNITED)
        stubGroupAndRequester(group, requester, isMember = false)

        // then
        assertThrows<ForbiddenException> {
            groupMemberService.listGroupMembers(groupId = group.id, requesterId = requester.id)
        }
    }

    @Test
    fun `ALL 그룹은 UNITED 회원도 조회 가능하다`() {
        // given
        val group = createGroup(grade = GroupAccessGrade.ALL)
        val requester = createMember(id = 2L, studentStatus = StudentStatus.UNITED)
        stubGroupAndRequester(group, requester, isMember = false)
        given(groupMemberRepository.findMemberListByGroupId(group.id)).willReturn(emptyList())

        // when (does not throw)
        groupMemberService.listGroupMembers(groupId = group.id, requesterId = requester.id)
    }

    @Test
    fun `delegateMaster는 그룹장이 다른 멤버에게 그룹장을 위임할 수 있다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val requester = createMember(id = 1L)
        val newMaster = createMember(id = 2L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(memberRepository.findById(2L)).willReturn(Optional.of(newMaster))
        given(memberRepository.findById(1L)).willReturn(Optional.of(requester))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 2L)).willReturn(true)
        given(groupMemberRepository.findByGroupId(100L)).willReturn(
            listOf(
                GroupMember(group = 100L, member = 1L),
                GroupMember(group = 100L, member = 2L),
            ),
        )

        // when
        groupMemberService.delegateMaster(groupId = 100L, requesterId = 1L, newMasterId = 2L)

        // then - group save: master 갱신 + changed_at/state 갱신
        val groupCaptor = argumentCaptor<Group>()
        verify(groupRepository).save(groupCaptor.capture())
        val saved = groupCaptor.firstValue
        assertEquals(2L, saved.master)

        // log 호출 (메시지 포맷: 레거시 그대로)
        verify(groupLogService).createLog(
            eq(100L),
            eq(1L),
            eq("그룹장이 공과대학 이름1에서 공과대학 이름2에게 위임되었습니다."),
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
    fun `delegateMaster는 그룹장이 아니면 403을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val newMaster = createMember(id = 2L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(memberRepository.findById(2L)).willReturn(Optional.of(newMaster))

        // then
        assertThrows<ForbiddenException> {
            // 요청자(99)는 master(1)가 아님
            groupMemberService.delegateMaster(groupId = 100L, requesterId = 99L, newMasterId = 2L)
        }
        verify(groupRepository, never()).save(any())
        verify(groupLogService, never()).createLog(any(), any(), any())
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `delegateMaster는 위임 대상이 그룹 멤버가 아니면 400을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val requester = createMember(id = 1L)
        val newMaster = createMember(id = 2L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(memberRepository.findById(2L)).willReturn(Optional.of(newMaster))
        given(memberRepository.findById(1L)).willReturn(Optional.of(requester))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 2L)).willReturn(false)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.delegateMaster(groupId = 100L, requesterId = 1L, newMasterId = 2L)
        }
        verify(groupRepository, never()).save(any())
    }

    @Test
    fun `delegateMaster는 자기 자신에게 위임하면 400을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val requester = createMember(id = 1L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(memberRepository.findById(1L)).willReturn(Optional.of(requester))

        // then
        assertThrows<BadRequestException> {
            groupMemberService.delegateMaster(groupId = 100L, requesterId = 1L, newMasterId = 1L)
        }
        verify(groupRepository, never()).save(any())
    }

    @Test
    fun `delegateMaster는 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findById(999L)).willReturn(Optional.empty())

        // then
        assertThrows<NotFoundException> {
            groupMemberService.delegateMaster(groupId = 999L, requesterId = 1L, newMasterId = 2L)
        }
    }

    @Test
    fun `delegateMaster는 위임 대상 회원이 존재하지 않으면 404를 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(memberRepository.findById(999L)).willReturn(Optional.empty())

        // then
        assertThrows<NotFoundException> {
            groupMemberService.delegateMaster(groupId = 100L, requesterId = 1L, newMasterId = 999L)
        }
    }

    @Test
    fun `joinGroup은 일반 카테고리 그룹 참여 시 멤버 추가·로그·알림·포인트 지급·상태 갱신을 수행한다`() {
        // given
        val group =
            createGroup(
                id = 100L,
                master = 5L,
                grade = GroupAccessGrade.ALL,
                state = GroupState.PROGRESS,
                allowJoin = true,
            )
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = false)

        // when
        groupMemberService.joinGroup(groupId = 100L, requesterId = 2L)

        // then
        verify(groupMemberRepository).save(100L, 2L)
        verify(groupLogService).createLog(eq(100L), eq(2L), eq("그룹에 참여했습니다."))

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
            point = eq(40),
            message = eq("<u>테스트 그룹</u> 그룹에 참여했습니다."),
        )
        verify(groupRepository).save(argThat<Group> { countMember == 1L })
    }

    @Test
    fun `joinGroup은 운영 카테고리 그룹이면 전 멤버에게 알림을 발송한다`() {
        // given - 운영 카테고리, 기존 멤버 3명, 참여자 본인 추가
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                category = GroupCategory.MANAGE,
                grade = GroupAccessGrade.ALL,
                state = GroupState.PROGRESS,
                allowJoin = true,
            )
        val requester = createMember(id = 5L)
        stubGroupAndRequester(group, requester, isMember = false)
        // findByGroupId는 add_member 이후 호출이라 참여자 포함된 결과를 반환해야 함
        given(groupMemberRepository.findByGroupId(100L)).willReturn(
            listOf(
                GroupMember(group = 100L, member = 1L),
                GroupMember(group = 100L, member = 3L),
                GroupMember(group = 100L, member = 5L),
            ),
        )

        // when
        groupMemberService.joinGroup(groupId = 100L, requesterId = 5L)

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
    fun `joinGroup은 그룹 활용 실습 그룹에는 포인트를 부여하지 않는다`() {
        // given
        val group =
            createGroup(
                id = GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID,
                master = 1L,
                grade = GroupAccessGrade.ALL,
                state = GroupState.PROGRESS,
                allowJoin = true,
            )
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = false)

        // when
        groupMemberService.joinGroup(groupId = GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID, requesterId = 2L)

        // then
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `joinGroup은 SUSPEND 상태 그룹에 참여하면 상태가 PROGRESS로 전환된다`() {
        // given
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                grade = GroupAccessGrade.ALL,
                state = GroupState.SUSPEND,
                allowJoin = true,
            )
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = false)

        // when (does not throw)
        groupMemberService.joinGroup(groupId = 100L, requesterId = 2L)

        // then
        verify(groupRepository).save(argThat<Group> { state == GroupState.PROGRESS })
    }

    @Test
    fun `joinGroup은 해당 그룹이 참여를 허용하지 않으면 403을 던진다`() {
        // given - 권한 통과 후 allow_join 단계에서 차단
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                grade = GroupAccessGrade.ALL,
                state = GroupState.PROGRESS,
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
    fun `joinGroup은 열람 권한 없으면 403을 던진다`() {
        // given - PRIVATE 그룹, 비멤버, 비-author (권한 단계에서 차단되므로 state/allowJoin 무관)
        val group = createGroup(id = 100L, master = 1L, author = 1L, grade = GroupAccessGrade.PRIVATE)
        val requester = createMember(id = 99L)
        stubGroupAndRequester(group, requester, isMember = false)

        // then
        assertThrows<ForbiddenException> {
            groupMemberService.joinGroup(groupId = 100L, requesterId = 99L)
        }
    }

    @Test
    fun `joinGroup은 이미 멤버인 경우 400을 던진다`() {
        // given - 권한·allow_join·state 모두 통과 후 이미 멤버 차단
        val group =
            createGroup(
                id = 100L,
                master = 1L,
                grade = GroupAccessGrade.ALL,
                state = GroupState.PROGRESS,
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
    fun `joinGroup은 PROGRESS, SUSPEND 외 상태에서 400을 던진다`() {
        // given
        listOf(GroupState.PENDING, GroupState.END_SUCCESS, GroupState.END_FAIL).forEach { state ->
            val group =
                createGroup(
                    id = 100L,
                    master = 1L,
                    grade = GroupAccessGrade.ALL,
                    state = state,
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
    fun `joinGroup은 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findById(999L)).willReturn(Optional.empty())

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
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(memberRepository.findById(5L)).willReturn(Optional.of(kickedMember))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 5L)).willReturn(true)

        // when
        groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 5L)

        // then
        verify(groupMemberRepository).delete(100L, 5L)
        verify(groupLogService).createLog(eq(100L), eq(1L), eq("공과대학 이름5 내보냈습니다."))
        verify(notificationService).createNotification(
            userId = eq(5L),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = eq("<a href=\"/group/100\"><u>테스트 그룹</u></a> 그룹에서 내보내졌습니다."),
            url = anyOrNull(),
        )
        verify(pointService).givePoint(
            targetUserId = eq(5L),
            point = eq(-40),
            message = eq("<u>테스트 그룹</u> 그룹에서 내보내졌습니다."),
        )
        verify(groupRepository).save(argThat<Group> { countMember == -1L })
    }

    @Test
    fun `kickMember는 그룹장이 아니면 403을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))

        // then
        assertThrows<ForbiddenException> {
            groupMemberService.kickMember(groupId = 100L, requesterId = 99L, kickedMemberId = 5L)
        }
    }

    @Test
    fun `kickMember는 운영 카테고리 그룹에서 그룹장이 아닌 회원도 멤버를 내보낼 수 있다`() {
        // given - 운영 카테고리는 그룹장 권한 체크 건너뜀
        val group = createGroup(id = 100L, master = 1L, category = GroupCategory.MANAGE)
        val kickedMember = createMember(id = 5L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(memberRepository.findById(5L)).willReturn(Optional.of(kickedMember))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 5L)).willReturn(true)

        // when - 그룹장(1L)이 아닌 99L이 요청
        groupMemberService.kickMember(groupId = 100L, requesterId = 99L, kickedMemberId = 5L)

        // then
        verify(groupMemberRepository).delete(100L, 5L)
    }

    @Test
    fun `kickMember는 자기 자신을 내보낼 수 없다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))

        // then
        assertThrows<BadRequestException> {
            groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 1L)
        }
    }

    @Test
    fun `kickMember는 그룹 활용 실습 그룹에서는 포인트를 차감하지 않는다`() {
        // given
        val group = createGroup(id = GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID, master = 1L)
        val kickedMember = createMember(id = 5L)
        given(groupRepository.findById(GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID)).willReturn(Optional.of(group))
        given(memberRepository.findById(5L)).willReturn(Optional.of(kickedMember))
        given(groupMemberRepository.existsByGroupIdAndMemberId(GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID, 5L)).willReturn(true)

        // when
        groupMemberService.kickMember(groupId = GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID, requesterId = 1L, kickedMemberId = 5L)

        // then
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `kickMember는 SUSPEND 상태 그룹에서 내보내면 상태가 PROGRESS로 전환된다`() {
        // given
        val group = createGroup(id = 100L, master = 1L, state = GroupState.SUSPEND)
        val kickedMember = createMember(id = 5L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(memberRepository.findById(5L)).willReturn(Optional.of(kickedMember))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 5L)).willReturn(true)

        // when
        groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 5L)

        // then
        verify(groupRepository).save(argThat<Group> { state == GroupState.PROGRESS })
    }

    @Test
    fun `kickMember는 대상이 그룹 멤버가 아니면 400을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        val kickedMember = createMember(id = 5L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(memberRepository.findById(5L)).willReturn(Optional.of(kickedMember))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 5L)).willReturn(false)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.kickMember(groupId = 100L, requesterId = 1L, kickedMemberId = 5L)
        }
    }

    @Test
    fun `kickMember는 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findById(999L)).willReturn(Optional.empty())

        // then
        assertThrows<NotFoundException> {
            groupMemberService.kickMember(groupId = 999L, requesterId = 1L, kickedMemberId = 5L)
        }
    }

    @Test
    fun `kickMember는 대상 회원이 존재하지 않으면 404를 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
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
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 5L)).willReturn(true)

        // when
        groupMemberService.leaveGroup(groupId = 100L, requesterId = 5L)

        // then
        verify(groupMemberRepository).delete(100L, 5L)
        verify(groupLogService).createLog(eq(100L), eq(5L), eq("그룹에서 나갔습니다."))

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

        verify(pointService).givePoint(
            targetUserId = eq(5L),
            point = eq(-40),
            message = eq("<u>테스트 그룹</u> 그룹에서 나갔습니다."),
        )
        verify(groupRepository).save(argThat<Group> { countMember == -1L })
    }

    @Test
    fun `leaveGroup은 운영 카테고리에서 잔여 멤버 전체에게 알림을 발송한다`() {
        // given - 운영 카테고리, master(1L)가 탈퇴, 잔여 멤버 2L, 3L
        val group = createGroup(id = 100L, master = 1L, category = GroupCategory.MANAGE)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 1L)).willReturn(true)
        // leave_member 이후의 멤버 목록 (탈퇴자 본인은 빠짐)
        given(groupMemberRepository.findByGroupId(100L)).willReturn(
            listOf(
                GroupMember(group = 100L, member = 2L),
                GroupMember(group = 100L, member = 3L),
            ),
        )

        // when
        groupMemberService.leaveGroup(groupId = 100L, requesterId = 1L)

        // then - 본인 1건 + 잔여 멤버 2건 = 3건
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
    fun `leaveGroup은 SUSPEND 상태 그룹에서 탈퇴하면 상태가 PROGRESS로 전환된다`() {
        // given
        val group = createGroup(id = 100L, master = 1L, state = GroupState.SUSPEND)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 5L)).willReturn(true)

        // when
        groupMemberService.leaveGroup(groupId = 100L, requesterId = 5L)

        // then
        verify(groupRepository).save(argThat<Group> { state == GroupState.PROGRESS })
    }

    @Test
    fun `leaveGroup은 그룹 활용 실습 그룹에서는 포인트를 차감하지 않는다`() {
        // given
        val group = createGroup(id = GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID, master = 1L)
        given(groupRepository.findById(GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID)).willReturn(Optional.of(group))
        given(groupMemberRepository.existsByGroupIdAndMemberId(GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID, 5L)).willReturn(true)

        // when
        groupMemberService.leaveGroup(groupId = GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID, requesterId = 5L)

        // then
        verify(pointService, never()).givePoint(any(), any(), any())
    }

    @Test
    fun `leaveGroup은 일반 카테고리에서 그룹장이 탈퇴하려 하면 400을 던진다`() {
        // given - 일반 카테고리, requester == master
        val group = createGroup(id = 100L, master = 1L, category = GroupCategory.STUDY)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 1L)).willReturn(true)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.leaveGroup(groupId = 100L, requesterId = 1L)
        }
    }

    @Test
    fun `leaveGroup은 비멤버가 탈퇴하려 하면 400을 던진다`() {
        // given
        val group = createGroup(id = 100L, master = 1L)
        given(groupRepository.findById(100L)).willReturn(Optional.of(group))
        given(groupMemberRepository.existsByGroupIdAndMemberId(100L, 99L)).willReturn(false)

        // then
        assertThrows<BadRequestException> {
            groupMemberService.leaveGroup(groupId = 100L, requesterId = 99L)
        }
    }

    @Test
    fun `leaveGroup은 그룹이 존재하지 않으면 404를 던진다`() {
        // given
        given(groupRepository.findById(999L)).willReturn(Optional.empty())

        // then
        assertThrows<NotFoundException> {
            groupMemberService.leaveGroup(groupId = 999L, requesterId = 1L)
        }
    }
}
