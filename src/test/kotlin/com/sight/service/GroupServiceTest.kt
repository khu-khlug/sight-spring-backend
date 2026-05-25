package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupState
import com.sight.repository.GroupBookmarkRepository
import com.sight.repository.GroupRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.util.Optional
import kotlin.test.assertFailsWith

class GroupServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupBookmarkRepository = mock<GroupBookmarkRepository>()
    private lateinit var groupService: GroupService

    private val baseGroup =
        Group(
            id = 1L,
            category = GroupCategory.STUDY,
            title = "테스트 그룹",
            author = 10L,
            master = 10L,
            state = GroupState.PROGRESS,
        )

    @BeforeEach
    fun setUp() {
        groupService = GroupService(groupRepository, groupBookmarkRepository)
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
    fun `addBookmark는 그룹이 존재하고 즐겨찾기하지 않은 경우 즐겨찾기를 추가한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupBookmarkRepository.existsByMemberAndGroup(10L, 1L)).willReturn(false)

        // when
        groupService.addBookmark(groupId = 1L, requesterId = 10L)

        // then
        verify(groupBookmarkRepository).save(argThat { member == 10L && group == 1L })
    }

    @Test
    fun `addBookmark는 이미 즐겨찾기한 그룹에 요청하면 422를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupBookmarkRepository.existsByMemberAndGroup(10L, 1L)).willReturn(true)

        // when & then
        assertFailsWith<UnprocessableEntityException> {
            groupService.addBookmark(groupId = 1L, requesterId = 10L)
        }
        verify(groupBookmarkRepository, never()).save(any())
    }

    @Test
    fun `addBookmark는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        // when & then
        assertFailsWith<NotFoundException> {
            groupService.addBookmark(groupId = 1L, requesterId = 10L)
        }
    }

    @Test
    fun `cancelBookmark는 즐겨찾기된 그룹에 요청하면 즐겨찾기를 취소한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupBookmarkRepository.existsByMemberAndGroup(10L, 1L)).willReturn(true)

        // when
        groupService.cancelBookmark(groupId = 1L, requesterId = 10L)

        // then
        verify(groupBookmarkRepository).deleteByMemberAndGroup(10L, 1L)
    }

    @Test
    fun `cancelBookmark는 즐겨찾기하지 않은 그룹에 요청하면 422를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupBookmarkRepository.existsByMemberAndGroup(10L, 1L)).willReturn(false)

        // when & then
        assertFailsWith<UnprocessableEntityException> {
            groupService.cancelBookmark(groupId = 1L, requesterId = 10L)
        }
        verify(groupBookmarkRepository, never()).deleteByMemberAndGroup(any(), any())
    }

    @Test
    fun `cancelBookmark는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        // when & then
        assertFailsWith<NotFoundException> {
            groupService.cancelBookmark(groupId = 1L, requesterId = 10L)
        }
    }
}
