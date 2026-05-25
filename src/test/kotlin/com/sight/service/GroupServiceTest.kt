package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupMember
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupState
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.Optional
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GroupServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val pointService = mock<PointService>()
    private val notificationService = mock<NotificationService>()
    private lateinit var groupService: GroupService

    private val baseGroup =
        Group(
            id = 1L,
            category = GroupCategory.STUDY,
            title = "테스트 그룹",
            author = 10L,
            master = 10L,
            state = GroupState.PROGRESS,
            portfolio = false,
        )

    @BeforeEach
    fun setUp() {
        groupService = GroupService(groupRepository, groupMemberRepository, pointService, notificationService)
        given(groupRepository.findGroups(any(), any(), any(), any(), any(), any())).willReturn(emptyList())
        given(groupRepository.countGroups(any(), any(), any())).willReturn(0L)
    }

    @Test
    fun `listGroups는 파라미터를 올바르게 전달한다`() {
        // when
        groupService.listGroups(offset = 20, limit = 50, bookmarked = null, joined = null, orderBy = null, requesterId = 1L)

        // then
        verify(groupRepository).findGroups(eq(20), eq(50), eq(null), eq(null), eq(null), eq(1L))
        verify(groupRepository).countGroups(eq(null), eq(null), eq(1L))
    }

    @Test
    fun `listGroups는 bookmarked가 true이면 bookmarked 파라미터를 전달한다`() {
        // when
        groupService.listGroups(offset = 0, limit = 10, bookmarked = true, joined = null, orderBy = null, requesterId = 123L)

        // then
        verify(groupRepository).findGroups(eq(0), eq(10), eq(null), eq(true), eq(null), eq(123L))
        verify(groupRepository).countGroups(eq(null), eq(true), eq(123L))
    }

    @Test
    fun `listGroups는 joined가 true이면 joined 파라미터를 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 6,
            bookmarked = null,
            joined = true,
            orderBy = GroupOrderBy.CHANGED_AT,
            requesterId = 123L,
        )

        // then
        verify(groupRepository).findGroups(eq(0), eq(6), eq(true), eq(null), eq(GroupOrderBy.CHANGED_AT), eq(123L))
        verify(groupRepository).countGroups(eq(true), eq(null), eq(123L))
    }

    @Test
    fun `listGroups는 joined와 bookmarked가 둘 다 true이면 둘 다 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = true,
            joined = true,
            orderBy = GroupOrderBy.CHANGED_AT,
            requesterId = 123L,
        )

        // then
        verify(groupRepository).findGroups(eq(0), eq(10), eq(true), eq(true), eq(GroupOrderBy.CHANGED_AT), eq(123L))
        verify(groupRepository).countGroups(eq(true), eq(true), eq(123L))
    }

    @Test
    fun `publishPortfolio는 그룹장이 미발행 그룹에 요청하면 포트폴리오 발행 여부를 true로 변경하고 멤버에게 포인트와 알림을 전송한다`() {
        // given
        val members = listOf(GroupMember(group = 1L, member = 20L), GroupMember(group = 1L, member = 30L))
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(members)

        // when
        val result = groupService.publishPortfolio(groupId = 1L, requesterId = 10L)

        // then
        assertTrue(result)
        verify(groupRepository).save(baseGroup.copy(portfolio = true))
        verify(pointService).givePoint(20L, 10, "포트폴리오 발행")
        verify(pointService).givePoint(30L, 10, "포트폴리오 발행")
    }

    @Test
    fun `publishPortfolio는 이미 발행된 그룹에 요청하면 400을 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup.copy(portfolio = true)))

        // when & then
        assertFailsWith<BadRequestException> {
            groupService.publishPortfolio(groupId = 1L, requesterId = 10L)
        }
    }

    @Test
    fun `publishPortfolio는 그룹장이 아닌 멤버가 요청하면 403을 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))

        // when & then
        assertFailsWith<ForbiddenException> {
            groupService.publishPortfolio(groupId = 1L, requesterId = 99L)
        }
    }

    @Test
    fun `publishPortfolio는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        // when & then
        assertFailsWith<NotFoundException> {
            groupService.publishPortfolio(groupId = 1L, requesterId = 10L)
        }
    }

    @Test
    fun `cancelPortfolio는 그룹장이 발행 중인 그룹에 요청하면 포트폴리오 발행 여부를 false로 변경하고 멤버에게 포인트와 알림을 전송한다`() {
        // given
        val publishedGroup = baseGroup.copy(portfolio = true)
        val members = listOf(GroupMember(group = 1L, member = 20L), GroupMember(group = 1L, member = 30L))
        given(groupRepository.findById(1L)).willReturn(Optional.of(publishedGroup))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(members)

        // when
        groupService.cancelPortfolio(groupId = 1L, requesterId = 10L)

        // then
        verify(groupRepository).save(publishedGroup.copy(portfolio = false))
        verify(pointService).givePoint(20L, -10, "포트폴리오 취소")
        verify(pointService).givePoint(30L, -10, "포트폴리오 취소")
    }

    @Test
    fun `cancelPortfolio는 발행되지 않은 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))

        // when & then
        assertFailsWith<NotFoundException> {
            groupService.cancelPortfolio(groupId = 1L, requesterId = 10L)
        }
    }

    @Test
    fun `cancelPortfolio는 그룹장이 아닌 멤버가 요청하면 403을 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup.copy(portfolio = true)))

        // when & then
        assertFailsWith<ForbiddenException> {
            groupService.cancelPortfolio(groupId = 1L, requesterId = 99L)
        }
    }

    @Test
    fun `cancelPortfolio는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        // when & then
        assertFailsWith<NotFoundException> {
            groupService.cancelPortfolio(groupId = 1L, requesterId = 10L)
        }
    }
}
