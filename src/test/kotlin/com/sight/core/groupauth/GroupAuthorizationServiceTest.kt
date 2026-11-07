package com.sight.core.groupauth

import com.sight.core.exception.ForbiddenException
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRecordAuthor
import com.sight.repository.GroupRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.util.Optional

class GroupAuthorizationServiceTest {
    private val groups = mock<GroupRepository>()
    private val members = mock<GroupMemberRepository>()
    private val records = mock<GroupAuthorizationRepository>()
    private val service = GroupAuthorizationService(groups, members, records)

    private fun authorize(
        roles: Set<GroupRole>,
        isManager: Boolean = false,
        allowManager: Boolean = false,
        requesterId: Long = 2L,
        recordId: Long? = 10L,
    ) = service.requirePermission(1L, recordId, requesterId, isManager, roles, allowManager)

    @Test
    fun `운영진 예외를 둔 API는 그룹 권한이 없는 운영진도 허용한다`() {
        given(groups.findById(1L)).willReturn(Optional.of(testGroup(visibility = GroupVisibility.PRIVATE)))
        authorize(setOf(GroupRole.LEADER), isManager = true, allowManager = true)
    }

    @Test
    fun `운영진 예외가 없는 API는 운영진도 그룹 권한이 있어야 한다`() {
        given(groups.findById(1L)).willReturn(Optional.of(testGroup(visibility = GroupVisibility.PRIVATE)))
        assertThrows<ForbiddenException> { authorize(setOf(GroupRole.LEADER), isManager = true) }
    }

    @Test
    fun `그룹장 권한은 그룹원 권한과 열람 권한을 포괄한다`() {
        given(groups.findById(1L)).willReturn(Optional.of(testGroup(visibility = GroupVisibility.PRIVATE)))
        authorize(setOf(GroupRole.MEMBER), requesterId = 1L)
        authorize(setOf(GroupRole.VIEWER), requesterId = 1L)
    }

    @Test
    fun `비공개 그룹은 생성자라도 그룹원이 아니면 열람할 수 없다`() {
        given(groups.findById(1L)).willReturn(Optional.of(testGroup(authorMemberId = 2L, visibility = GroupVisibility.PRIVATE)))
        assertThrows<ForbiddenException> { authorize(setOf(GroupRole.VIEWER)) }
    }

    @Test
    fun `모든 공개 범위에서 그룹원은 그룹을 열람할 수 있다`() {
        given(members.existsByGroupIdAndMemberId(1L, 2L)).willReturn(true)
        GroupVisibility.entries.forEach { visibility ->
            val category = if (visibility == GroupVisibility.MANAGER_ONLY) GroupCategory.MANAGE else GroupCategory.STUDY
            given(groups.findById(1L)).willReturn(Optional.of(testGroup(category = category, visibility = visibility)))
            authorize(setOf(GroupRole.VIEWER), isManager = visibility == GroupVisibility.MANAGER_ONLY)
        }
    }

    @Test
    fun `회원 공개 그룹은 비그룹원 중 교류 회원을 제외한 회원이 열람할 수 있다`() {
        given(groups.findById(1L)).willReturn(Optional.of(testGroup(visibility = GroupVisibility.MEMBER_ONLY)))
        given(records.isUnitedMember(2L)).willReturn(true)
        assertThrows<ForbiddenException> { authorize(setOf(GroupRole.VIEWER)) }
        given(records.isUnitedMember(2L)).willReturn(false)
        authorize(setOf(GroupRole.VIEWER))
    }

    @Test
    fun `완전 공개 그룹은 모든 회원이 열람할 수 있다`() {
        given(groups.findById(1L)).willReturn(Optional.of(testGroup(visibility = GroupVisibility.PUBLIC)))
        authorize(setOf(GroupRole.VIEWER))
    }

    @Test
    fun `운영진 공개 그룹은 운영진만 열람할 수 있다`() {
        given(
            groups.findById(1L),
        ).willReturn(Optional.of(testGroup(category = GroupCategory.MANAGE, visibility = GroupVisibility.MANAGER_ONLY)))
        assertThrows<ForbiddenException> { authorize(setOf(GroupRole.VIEWER)) }
        authorize(setOf(GroupRole.VIEWER), isManager = true)
    }

    @Test
    fun `운영진 그룹권한은 운영 그룹에서 운영진에게만 주어진다`() {
        given(groups.findById(1L)).willReturn(Optional.of(testGroup()))
        given(records.isManageGroup(1L)).willReturn(true)
        authorize(setOf(GroupRole.MANAGER), isManager = true)
        assertThrows<ForbiddenException> { authorize(setOf(GroupRole.MANAGER)) }
        given(records.isManageGroup(1L)).willReturn(false)
        assertThrows<ForbiddenException> { authorize(setOf(GroupRole.MANAGER), isManager = true) }
    }

    @Test
    fun `기록 작성자 권한은 그룹원이 그 그룹에 속한 본인 기록에 대해서만 가진다`() {
        given(groups.findById(1L)).willReturn(Optional.of(testGroup()))
        given(records.findRecordAuthorById(10L)).willReturn(GroupRecordAuthor(1L, 2L))
        assertThrows<ForbiddenException> { authorize(setOf(GroupRole.WRITER)) }
        given(members.existsByGroupIdAndMemberId(1L, 2L)).willReturn(true)
        authorize(setOf(GroupRole.WRITER))
        given(records.findRecordAuthorById(10L)).willReturn(GroupRecordAuthor(2L, 2L))
        assertThrows<ForbiddenException> { authorize(setOf(GroupRole.WRITER)) }
        given(records.findRecordAuthorById(10L)).willReturn(GroupRecordAuthor(1L, 3L))
        assertThrows<ForbiddenException> { authorize(setOf(GroupRole.WRITER)) }
    }
}
