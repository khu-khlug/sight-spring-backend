package com.sight.service

import com.sight.domain.group.GroupOrderBy
import com.sight.repository.GroupRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GroupServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private lateinit var groupService: GroupService

    @BeforeEach
    fun setUp() {
        groupService = GroupService(groupRepository)
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
}
