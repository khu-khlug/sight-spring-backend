package com.sight.service

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.group.GroupCategory
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import com.sight.repository.dto.GroupMemberListDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.util.Optional

class GroupMemberServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val memberRepository = mock<MemberRepository>()
    private val groupMemberService =
        GroupMemberService(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            memberRepository = memberRepository,
        )

    private fun createGroup(
        id: Long = 100L,
        master: Long = 1L,
        grade: GroupAccessGrade = GroupAccessGrade.MEMBER,
    ) = Group(
        id = id,
        category = GroupCategory.STUDY,
        title = "테스트 그룹",
        author = master,
        master = master,
        grade = grade,
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
    fun `PRIVATE 그룹은 비멤버가 조회하면 403을 던진다`() {
        // given
        val group = createGroup(grade = GroupAccessGrade.PRIVATE)
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
}
