package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupMember
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import com.sight.repository.dto.GroupMemberListDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
    private val groupMemberService =
        GroupMemberService(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            memberRepository = memberRepository,
            groupLogService = groupLogService,
            notificationService = notificationService,
        )

    private fun createGroup(
        id: Long = 100L,
        master: Long = 1L,
        author: Long = master,
        grade: GroupAccessGrade = GroupAccessGrade.MEMBER,
        changedAt: LocalDateTime = LocalDateTime.now(),
    ) = Group(
        id = id,
        category = GroupCategory.STUDY,
        title = "테스트 그룹",
        author = author,
        master = master,
        grade = grade,
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
    fun `PRIVATE 그룹은 비멤버라도 author면 조회 가능하다`() {
        // given: 위임 이후 master가 다른 사람이어도 최초 생성자(author)는 열람 가능
        val group = createGroup(grade = GroupAccessGrade.PRIVATE, master = 10L, author = 2L)
        val requester = createMember(id = 2L)
        stubGroupAndRequester(group, requester, isMember = false)
        given(groupMemberRepository.findMemberListByGroupId(group.id)).willReturn(emptyList())

        // when (does not throw)
        groupMemberService.listGroupMembers(groupId = group.id, requesterId = requester.id)
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
        val oldChangedAt = LocalDateTime.of(2020, 1, 1, 0, 0)
        val group = createGroup(id = 100L, master = 1L, changedAt = oldChangedAt)
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

        // then - group save: master 갱신 + changedAt 갱신
        val groupCaptor = argumentCaptor<Group>()
        verify(groupRepository).save(groupCaptor.capture())
        val saved = groupCaptor.firstValue
        assertEquals(2L, saved.master)
        assertTrue(saved.changedAt.isAfter(oldChangedAt))

        // log 호출 (메시지 포맷: 레거시 그대로)
        verify(groupLogService).createLog(
            eq(100L),
            eq(1L),
            eq("그룹장이 공과대학 이름1에서 공과대학 이름2에게 위임되었습니다."),
        )

        // 알림: 멤버 2명 each, content 포맷 검증
        val contentCaptor = argumentCaptor<String>()
        verify(notificationService, times(2)).createNotification(
            userId = any(),
            category = eq(NotificationCategory.GROUP),
            title = eq(""),
            content = contentCaptor.capture(),
            url = anyOrNull(),
        )
        val expectedContent =
            "<a href=\"/group/100#member\"><u>테스트 그룹</u></a> 그룹장이 이름2에게 위임되었습니다."
        contentCaptor.allValues.forEach { assertEquals(expectedContent, it) }
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
}
