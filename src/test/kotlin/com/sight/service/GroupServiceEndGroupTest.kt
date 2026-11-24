package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogExpointChange
import com.sight.domain.group.GroupPolicy
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.domain.group.testGroupMember
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupBookmarkRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.util.Optional

class GroupServiceEndGroupTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val pointService = mock<PointService>()
    private val notificationService = mock<NotificationService>()
    private val groupLogService = mock<GroupLogService>()
    private val groupService =
        GroupService(
            integerIdService = mock<IntegerIdService>(),
            groupRepository = groupRepository,
            groupBookmarkRepository = mock<GroupBookmarkRepository>(),
            groupMemberRepository = groupMemberRepository,
            pointService = pointService,
            notificationService = notificationService,
            groupLogService = groupLogService,
        )

    private fun stubGroup(
        groupId: Long = 1L,
        status: GroupStatus = GroupStatus.PROGRESS,
        memberIds: List<Long> = listOf(10L, 11L),
    ) {
        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup(id = groupId, title = "<b>그룹</b>", status = status)))
        given(groupRepository.endProgressById(any(), any(), any())).willReturn(1)
        given(groupMemberRepository.findByGroupId(groupId)).willReturn(memberIds.map { testGroupMember(groupId = groupId, memberId = it) })
    }

    private fun verifyNoSideEffects() {
        verify(groupRepository, never()).endProgressById(any(), any(), any())
        verifyNoInteractions(pointService, notificationService, groupLogService)
    }

    @Test
    fun `성공으로 종료하면 그룹원 전체에게 90 경험치와 알림을 보내고 로그에 변동을 남긴다`() {
        stubGroup()

        groupService.endGroup(groupId = 1L, requesterId = 10L, result = GroupStatus.SUCCESS)

        verify(groupRepository).endProgressById(eq(1L), eq(GroupStatus.SUCCESS), eq(GroupStatus.PROGRESS))
        verify(groupLogService).createLog(
            groupId = eq(1L),
            actorMemberId = eq(10L),
            data = eq(GroupLogData.GroupEndedSuccess),
            expointChange = eq(GroupLogExpointChange(targetId = "1", point = 90, memberIds = listOf(10L, 11L))),
        )
        listOf(10L, 11L).forEach { memberId ->
            verify(pointService).givePoint(
                targetUserId = eq(memberId),
                point = eq(90),
                message = argThat { contains("종료(성공)") },
            )
            verify(notificationService).createNotification(
                userId = eq(memberId),
                category = eq(NotificationCategory.GROUP),
                title = eq(""),
                // 그룹 이름의 HTML은 이스케이프한다.
                content = argThat { contains("종료(성공)") && contains("&lt;b&gt;그룹&lt;/b&gt;") },
                url = anyOrNull(),
            )
        }
        // 종료는 활동이 아니므로 최근 활동 일시를 갱신하지 않는다.
        verify(groupRepository, never()).recordActivityById(any(), any())
    }

    @Test
    fun `실패로 종료하면 그룹원 전체에게 70 경험치를 보내고 실패 로그를 남긴다`() {
        stubGroup()

        groupService.endGroup(groupId = 1L, requesterId = 20L, result = GroupStatus.FAIL)

        verify(groupRepository).endProgressById(eq(1L), eq(GroupStatus.FAIL), eq(GroupStatus.PROGRESS))
        verify(groupLogService).createLog(
            groupId = eq(1L),
            actorMemberId = eq(20L),
            data = eq(GroupLogData.GroupEndedFail),
            expointChange = eq(GroupLogExpointChange(targetId = "1", point = 70, memberIds = listOf(10L, 11L))),
        )
        listOf(10L, 11L).forEach { memberId ->
            verify(pointService).givePoint(
                targetUserId = eq(memberId),
                point = eq(70),
                message = argThat { contains("종료(실패)") },
            )
        }
    }

    @Test
    fun `실습 그룹은 경험치를 지급하지 않지만 종료 로그와 알림은 남긴다`() {
        stubGroup(groupId = GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID)

        groupService.endGroup(groupId = GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID, requesterId = 10L, result = GroupStatus.SUCCESS)

        verify(groupRepository).endProgressById(eq(GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID), eq(GroupStatus.SUCCESS), eq(GroupStatus.PROGRESS))
        verify(pointService, never()).givePoint(any(), any(), any())
        // 경험치를 지급하지 않았으므로 로그에 변동을 남기지 않는다.
        verify(groupLogService).createLog(
            groupId = eq(GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID),
            actorMemberId = eq(10L),
            data = eq(GroupLogData.GroupEndedSuccess),
            expointChange = isNull(),
        )
        listOf(10L, 11L).forEach { memberId ->
            verify(notificationService).createNotification(
                userId = eq(memberId),
                category = eq(NotificationCategory.GROUP),
                title = eq(""),
                content = argThat { contains("종료(성공)") },
                url = anyOrNull(),
            )
        }
    }

    @Test
    fun `그룹원이 없으면 경험치와 알림 없이 종료하고 변동 기록을 남기지 않는다`() {
        stubGroup(memberIds = emptyList())

        groupService.endGroup(groupId = 1L, requesterId = 10L, result = GroupStatus.SUCCESS)

        verify(groupLogService).createLog(
            groupId = eq(1L),
            actorMemberId = eq(10L),
            data = eq(GroupLogData.GroupEndedSuccess),
            expointChange = isNull(),
        )
        verifyNoInteractions(pointService, notificationService)
    }

    @Test
    fun `종료 결과가 성공 또는 실패가 아니면 400이다`() {
        stubGroup()

        listOf(GroupStatus.PROGRESS, GroupStatus.STOP).forEach { status ->
            assertThrows<BadRequestException> { groupService.endGroup(1L, 10L, status) }
        }
        verifyNoSideEffects()
    }

    @Test
    fun `중단되거나 이미 종료된 그룹은 400이고 경험치·알림·로그를 남기지 않는다`() {
        listOf(GroupStatus.STOP, GroupStatus.SUCCESS, GroupStatus.FAIL).forEach { status ->
            stubGroup(status = status)

            assertThrows<BadRequestException> { groupService.endGroup(1L, 10L, GroupStatus.SUCCESS) }
        }
        verifyNoSideEffects()
    }

    @Test
    fun `동시에 종료해 상태 변경에 실패한 요청은 경험치·알림·로그를 남기지 않는다`() {
        stubGroup()
        given(groupRepository.endProgressById(any(), any(), any())).willReturn(0)

        assertThrows<BadRequestException> { groupService.endGroup(1L, 10L, GroupStatus.SUCCESS) }

        verifyNoInteractions(pointService, notificationService, groupLogService)
    }

    @Test
    fun `그룹이 없으면 404이다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        assertThrows<NotFoundException> { groupService.endGroup(1L, 10L, GroupStatus.SUCCESS) }
        verifyNoSideEffects()
    }
}
