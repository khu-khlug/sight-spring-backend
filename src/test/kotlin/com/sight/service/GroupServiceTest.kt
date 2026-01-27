package com.sight.service

import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupState
import com.sight.repository.GroupRepository
import com.sight.repository.projection.GroupListProjection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import kotlin.test.assertEquals

class GroupServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private lateinit var groupService: GroupService

    @BeforeEach
    fun setUp() {
        groupService = GroupService(groupRepository)
    }

    @Test
    fun `listGroups는 bookmarked가 null이면 전체 그룹을 조회한다`() {
        // given
        val offset = 0
        val limit = 10
        val mockProjection =
            createMockProjection(
                id = 1L,
                category = GroupCategory.STUDY.value,
                title = "Test Group",
                state = GroupState.PROGRESS.value,
                countMember = 5L,
                allowJoin = 1,
                createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
            )
        given(groupRepository.findAllGroups(offset, limit)).willReturn(listOf(mockProjection))
        given(groupRepository.countAllGroups()).willReturn(1L)

        // when
        val result = groupService.listGroups(offset, limit, bookmarked = null, requesterId = 1L)

        // then
        assertEquals(1L, result.count)
        assertEquals(1, result.groups.size)
        verify(groupRepository).findAllGroups(offset, limit)
        verify(groupRepository).countAllGroups()
    }

    @Test
    fun `listGroups는 bookmarked가 false이면 전체 그룹을 조회한다`() {
        // given
        val offset = 0
        val limit = 10
        given(groupRepository.findAllGroups(offset, limit)).willReturn(emptyList())
        given(groupRepository.countAllGroups()).willReturn(0L)

        // when
        val result = groupService.listGroups(offset, limit, bookmarked = false, requesterId = 1L)

        // then
        assertEquals(0L, result.count)
        assertEquals(0, result.groups.size)
        verify(groupRepository).findAllGroups(offset, limit)
        verify(groupRepository).countAllGroups()
    }

    @Test
    fun `listGroups는 bookmarked가 true이면 즐겨찾기 그룹을 조회한다`() {
        // given
        val offset = 0
        val limit = 10
        val requesterId = 123L
        val mockProjection =
            createMockProjection(
                id = 2L,
                category = GroupCategory.PROJECT.value,
                title = "Bookmarked Group",
                state = GroupState.PENDING.value,
                countMember = 3L,
                allowJoin = 0,
                createdAt = LocalDateTime.of(2024, 2, 1, 0, 0),
            )
        given(groupRepository.findBookmarkedGroups(requesterId, offset, limit)).willReturn(listOf(mockProjection))
        given(groupRepository.countBookmarkedGroups(requesterId)).willReturn(1L)

        // when
        val result = groupService.listGroups(offset, limit, bookmarked = true, requesterId = requesterId)

        // then
        assertEquals(1L, result.count)
        assertEquals(1, result.groups.size)
        verify(groupRepository).findBookmarkedGroups(requesterId, offset, limit)
        verify(groupRepository).countBookmarkedGroups(requesterId)
    }

    @Test
    fun `listGroups는 bookmarked가 true이지만 requesterId가 null이면 전체 그룹을 조회한다`() {
        // given
        val offset = 0
        val limit = 10
        given(groupRepository.findAllGroups(offset, limit)).willReturn(emptyList())
        given(groupRepository.countAllGroups()).willReturn(0L)

        // when
        groupService.listGroups(offset, limit, bookmarked = true, requesterId = null)

        // then
        verify(groupRepository).findAllGroups(offset, limit)
        verify(groupRepository).countAllGroups()
    }

    @Test
    fun `listGroups는 페이지네이션 파라미터를 올바르게 전달한다`() {
        // given
        val offset = 20
        val limit = 50
        given(groupRepository.findAllGroups(offset, limit)).willReturn(emptyList())
        given(groupRepository.countAllGroups()).willReturn(100L)

        // when
        val result = groupService.listGroups(offset, limit, bookmarked = null, requesterId = null)

        // then
        assertEquals(100L, result.count)
        verify(groupRepository).findAllGroups(20, 50)
    }

    @Test
    fun `listGroups는 그룹장 정보를 포함하여 반환한다`() {
        // given
        val offset = 0
        val limit = 10
        val mockProjection =
            createMockProjection(
                id = 1L,
                category = GroupCategory.STUDY.value,
                title = "Test Group",
                state = GroupState.PROGRESS.value,
                countMember = 5L,
                allowJoin = 1,
                createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
                leaderUserId = 100L,
                leaderName = "홍길동",
            )
        given(groupRepository.findAllGroups(offset, limit)).willReturn(listOf(mockProjection))
        given(groupRepository.countAllGroups()).willReturn(1L)

        // when
        val result = groupService.listGroups(offset, limit, bookmarked = null, requesterId = 1L)

        // then
        assertEquals(1, result.groups.size)
        val group = result.groups[0]
        assertEquals(100L, group.leader.userId)
        assertEquals("홍길동", group.leader.name)
    }

    private fun createMockProjection(
        id: Long,
        category: String,
        title: String,
        state: String,
        countMember: Long,
        allowJoin: Byte,
        createdAt: LocalDateTime,
        leaderUserId: Long = 1L,
        leaderName: String = "테스트유저",
    ): GroupListProjection =
        object : GroupListProjection {
            override val id: Long = id
            override val category: String = category
            override val title: String = title
            override val state: String = state
            override val countMember: Long = countMember
            override val allowJoin: Byte = allowJoin
            override val createdAt: LocalDateTime = createdAt
            override val leaderUserId: Long = leaderUserId
            override val leaderName: String = leaderName
        }
}
