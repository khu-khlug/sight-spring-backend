package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupState
import com.sight.repository.GroupRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private lateinit var groupService: GroupService

    @BeforeEach
    fun setUp() {
        groupService = GroupService(groupRepository)
        given(
            groupRepository.findGroups(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).willReturn(emptyList())
        given(
            groupRepository.countGroups(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).willReturn(0L)
    }

    @Test
    fun `listGroups는 파라미터를 올바르게 전달한다`() {
        // when
        groupService.listGroups(
            offset = 20,
            limit = 50,
            bookmarked = null,
            joined = null,
            categories = null,
            state = null,
            interest = null,
            keyword = null,
            orderBy = null,
            requesterId = 1L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(20),
            eq(50),
            eq(null),
            eq(null),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(null),
            eq(1L),
        )
        verify(groupRepository).countGroups(
            eq(null),
            eq(null),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(1L),
        )
    }

    @Test
    fun `listGroups는 bookmarked가 true이면 bookmarked 파라미터를 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = true,
            joined = null,
            categories = null,
            state = null,
            interest = null,
            keyword = null,
            orderBy = null,
            requesterId = 123L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(10),
            eq(null),
            eq(true),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(null),
            eq(123L),
        )
        verify(groupRepository).countGroups(
            eq(null),
            eq(true),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(123L),
        )
    }

    @Test
    fun `listGroups는 joined가 true이면 joined 파라미터를 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 6,
            bookmarked = null,
            joined = true,
            categories = null,
            state = null,
            interest = null,
            keyword = null,
            orderBy = GroupOrderBy.CHANGED_AT,
            requesterId = 123L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(6),
            eq(true),
            eq(null),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(GroupOrderBy.CHANGED_AT),
            eq(123L),
        )
        verify(groupRepository).countGroups(
            eq(true),
            eq(null),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(123L),
        )
    }

    @Test
    fun `listGroups는 joined와 bookmarked가 둘 다 true이면 둘 다 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = true,
            joined = true,
            categories = null,
            state = null,
            interest = null,
            keyword = null,
            orderBy = GroupOrderBy.CHANGED_AT,
            requesterId = 123L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(10),
            eq(true),
            eq(true),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(GroupOrderBy.CHANGED_AT),
            eq(123L),
        )
        verify(groupRepository).countGroups(
            eq(true),
            eq(true),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(123L),
        )
    }

    // --- 카테고리 필터 테스트 ---

    @Test
    fun `category=study로 조회하면 스터디 카테고리를 Repository에 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = null,
            joined = null,
            categories = listOf("study"),
            state = null,
            interest = null,
            keyword = null,
            orderBy = null,
            requesterId = 1L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(10),
            eq(null),
            eq(null),
            eq(listOf(GroupCategory.STUDY)),
            isNull(),
            isNull(),
            isNull(),
            eq(null),
            eq(1L),
        )
    }

    @Test
    fun `category=study,project로 조회하면 두 카테고리를 Repository에 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = null,
            joined = null,
            categories = listOf("study", "project"),
            state = null,
            interest = null,
            keyword = null,
            orderBy = null,
            requesterId = 1L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(10),
            eq(null),
            eq(null),
            eq(listOf(GroupCategory.STUDY, GroupCategory.PROJECT)),
            isNull(),
            isNull(),
            isNull(),
            eq(null),
            eq(1L),
        )
    }

    @Test
    fun `category 파라미터에 유효하지 않은 값을 전달하면 BadRequestException을 발생시킨다`() {
        // when & then
        val exception =
            assertThrows<BadRequestException> {
                groupService.listGroups(
                    offset = 0,
                    limit = 10,
                    bookmarked = null,
                    joined = null,
                    categories = listOf("invalid_category"),
                    state = null,
                    interest = null,
                    keyword = null,
                    orderBy = null,
                    requesterId = 1L,
                )
            }

        assertTrue(exception.message.contains("invalid_category"))
    }

    // --- 상태 필터 테스트 ---

    @Test
    fun `state=progress로 조회하면 진행 중 상태를 Repository에 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = null,
            joined = null,
            categories = null,
            state = "progress",
            interest = null,
            keyword = null,
            orderBy = null,
            requesterId = 1L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(10),
            eq(null),
            eq(null),
            isNull(),
            eq(GroupState.PROGRESS),
            isNull(),
            isNull(),
            eq(null),
            eq(1L),
        )
    }

    @Test
    fun `state=end-success로 조회하면 종료 성공 상태를 Repository에 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = null,
            joined = null,
            categories = null,
            state = "end-success",
            interest = null,
            keyword = null,
            orderBy = null,
            requesterId = 1L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(10),
            eq(null),
            eq(null),
            isNull(),
            eq(GroupState.END_SUCCESS),
            isNull(),
            isNull(),
            eq(null),
            eq(1L),
        )
    }

    @Test
    fun `state 파라미터에 유효하지 않은 값을 전달하면 BadRequestException을 발생시킨다`() {
        // when & then
        val exception =
            assertThrows<BadRequestException> {
                groupService.listGroups(
                    offset = 0,
                    limit = 10,
                    bookmarked = null,
                    joined = null,
                    categories = null,
                    state = "invalid_state",
                    interest = null,
                    keyword = null,
                    orderBy = null,
                    requesterId = 1L,
                )
            }

        assertTrue(exception.message.contains("invalid_state"))
    }

    // --- interest / keyword 필터 테스트 ---

    @Test
    fun `interest 값을 그대로 Repository에 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = null,
            joined = null,
            categories = null,
            state = null,
            interest = "웹",
            keyword = null,
            orderBy = null,
            requesterId = 1L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(10),
            eq(null),
            eq(null),
            isNull(),
            isNull(),
            eq("웹"),
            isNull(),
            eq(null),
            eq(1L),
        )
    }

    @Test
    fun `keyword 값을 그대로 Repository에 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = null,
            joined = null,
            categories = null,
            state = null,
            interest = null,
            keyword = "Spring",
            orderBy = null,
            requesterId = 1L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(10),
            eq(null),
            eq(null),
            isNull(),
            isNull(),
            isNull(),
            eq("Spring"),
            eq(null),
            eq(1L),
        )
    }

    // --- 복합 필터 테스트 ---

    @Test
    fun `여러 필터를 동시에 사용할 수 있다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = null,
            joined = null,
            categories = listOf("study", "project"),
            state = "progress",
            interest = "웹",
            keyword = "Spring",
            orderBy = GroupOrderBy.CHANGED_AT,
            requesterId = 1L,
        )

        // then
        verify(groupRepository).findGroups(
            eq(0),
            eq(10),
            eq(null),
            eq(null),
            eq(listOf(GroupCategory.STUDY, GroupCategory.PROJECT)),
            eq(GroupState.PROGRESS),
            eq("웹"),
            eq("Spring"),
            eq(GroupOrderBy.CHANGED_AT),
            eq(1L),
        )
        verify(groupRepository).countGroups(
            eq(null),
            eq(null),
            eq(listOf(GroupCategory.STUDY, GroupCategory.PROJECT)),
            eq(GroupState.PROGRESS),
            eq("웹"),
            eq("Spring"),
            eq(1L),
        )
    }

    @Test
    fun `count는 페이지네이션과 무관하게 필터 조건에 맞는 전체 그룹 수를 반환한다`() {
        // given
        given(
            groupRepository.countGroups(
                eq(null),
                eq(null),
                eq(listOf(GroupCategory.STUDY)),
                eq(GroupState.PROGRESS),
                eq(null),
                eq(null),
                eq(1L),
            ),
        ).willReturn(42L)

        // when
        val result =
            groupService.listGroups(
                offset = 20,
                limit = 5,
                bookmarked = null,
                joined = null,
                categories = listOf("study"),
                state = "progress",
                interest = null,
                keyword = null,
                orderBy = null,
                requesterId = 1L,
            )

        // then
        assertEquals(42L, result.count)
    }
}
