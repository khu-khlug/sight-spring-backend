package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupInterest
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.domain.group.testGroupMember
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupBookmarkRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberPointRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GroupServiceTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupBookmarkRepository = mock<GroupBookmarkRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val memberPointRepository = mock<MemberPointRepository>()
    private val groupLogService = mock<GroupLogService>()
    private val notificationService = mock<NotificationService>()
    private lateinit var groupService: GroupService

    private val baseGroup =
        testGroup(
            id = 1L,
            category = GroupCategory.STUDY,
            title = "테스트 그룹",
            leaderMemberId = 10L,
            status = GroupStatus.PROGRESS,
        )

    @BeforeEach
    fun setUp() {
        groupService =
            GroupService(
                mock<IntegerIdService>().apply { given(nextId()).willReturn(900000000000000001L) },
                groupRepository,
                groupBookmarkRepository,
                groupMemberRepository,
                memberPointRepository,
                notificationService,
                groupLogService,
            )
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
        given(groupRepository.updatePortfolioPublishedById(any(), any(), any(), any())).willReturn(1)
        given(memberPointRepository.addPoints(any(), any())).willReturn(1)
        given(groupBookmarkRepository.lockMemberById(10L)).willReturn(10L)
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
            eq(GroupStatus.PROGRESS),
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
            eq(GroupStatus.SUCCESS),
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

    @Test
    fun `interest 값을 관심 분야 enum으로 바꿔 Repository에 전달한다`() {
        // when
        groupService.listGroups(
            offset = 0,
            limit = 10,
            bookmarked = null,
            joined = null,
            categories = null,
            state = null,
            interest = "WEB_APP_SERVICE",
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
            eq(GroupInterest.WEB_APP_SERVICE),
            isNull(),
            eq(null),
            eq(1L),
        )
    }

    @Test
    fun `interest가 관심 분야 enum 이름이 아니면 Repository를 호출하지 않고 빈 목록을 반환한다`() {
        // when
        val result =
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
        assertEquals(0L, result.count)
        assertTrue(result.groups.isEmpty())
        verify(groupRepository, never()).findGroups(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `state=pending이면 해당하는 그룹 상태가 없으므로 Repository를 호출하지 않고 빈 목록을 반환한다`() {
        // when
        val result =
            groupService.listGroups(
                offset = 0,
                limit = 10,
                bookmarked = null,
                joined = null,
                categories = null,
                state = "pending",
                interest = null,
                keyword = null,
                orderBy = null,
                requesterId = 1L,
            )

        // then
        assertEquals(0L, result.count)
        assertTrue(result.groups.isEmpty())
        verify(groupRepository, never()).countGroups(any(), any(), any(), any(), any(), any(), any())
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
            interest = "WEB_APP_SERVICE",
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
            eq(GroupStatus.PROGRESS),
            eq(GroupInterest.WEB_APP_SERVICE),
            eq("Spring"),
            eq(GroupOrderBy.CHANGED_AT),
            eq(1L),
        )
        verify(groupRepository).countGroups(
            eq(null),
            eq(null),
            eq(listOf(GroupCategory.STUDY, GroupCategory.PROJECT)),
            eq(GroupStatus.PROGRESS),
            eq(GroupInterest.WEB_APP_SERVICE),
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
                eq(GroupStatus.PROGRESS),
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

    @ParameterizedTest
    @EnumSource(value = GroupStatus::class, names = ["PROGRESS", "SUCCESS", "FAIL"])
    fun `즐겨찾기를 추가하고 요청자에게 알림을 한 건 생성한다`(status: GroupStatus) {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(testGroup(status = status)))
        given(groupBookmarkRepository.existsByMemberIdAndGroupId(10L, 1L)).willReturn(false)

        // when
        groupService.addBookmark(groupId = 1L, requesterId = 10L)

        // then
        verify(groupBookmarkRepository).saveAndFlush(argThat { memberId == 10L && groupId == 1L })
        verify(notificationService).createNotification(
            userId = 10L,
            category = NotificationCategory.GROUP,
            title = "그룹 즐겨찾기 추가",
            content = "‘테스트 그룹’ 그룹을 즐겨찾기에 추가했습니다.",
            url = null,
        )
    }

    @Test
    fun `중단된 그룹에는 즐겨찾기와 알림을 생성하지 않는다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(testGroup(status = GroupStatus.STOP)))

        assertThrows<BadRequestException> {
            groupService.addBookmark(groupId = 1L, requesterId = 10L)
        }

        verify(groupBookmarkRepository, never()).saveAndFlush(any())
        verifyNoInteractions(notificationService)
    }

    @Test
    fun `addBookmark는 이미 즐겨찾기한 그룹에 요청하면 422를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupBookmarkRepository.existsByMemberIdAndGroupId(10L, 1L)).willReturn(true)

        // when & then
        assertThrows<UnprocessableEntityException> {
            groupService.addBookmark(groupId = 1L, requesterId = 10L)
        }
        verify(groupBookmarkRepository, never()).saveAndFlush(any())
        verifyNoInteractions(notificationService)
    }

    @Test
    fun `addBookmark는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            groupService.addBookmark(groupId = 1L, requesterId = 10L)
        }
        verify(groupBookmarkRepository, never()).saveAndFlush(any())
        verifyNoInteractions(notificationService)
    }

    @Test
    fun `즐겨찾기 저장 실패는 그대로 전파하고 알림을 생성하지 않는다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupBookmarkRepository.existsByMemberIdAndGroupId(10L, 1L)).willReturn(false)
        val failure = DataIntegrityViolationException("즐겨찾기 저장 실패")
        given(groupBookmarkRepository.saveAndFlush(any())).willThrow(failure)

        val thrown = assertThrows<DataIntegrityViolationException> {
            groupService.addBookmark(groupId = 1L, requesterId = 10L)
        }

        assertEquals(failure, thrown)
        verifyNoInteractions(notificationService)
    }

    @ParameterizedTest
    @EnumSource(value = GroupStatus::class, names = ["PROGRESS", "SUCCESS", "FAIL"])
    fun `즐겨찾기를 취소하면 요청자에게 해제 알림을 한 건 생성한다`(status: GroupStatus) {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(testGroup(status = status)))
        given(groupBookmarkRepository.deleteByMemberIdAndGroupId(10L, 1L)).willReturn(1)

        // when
        groupService.cancelBookmark(groupId = 1L, requesterId = 10L)

        // then
        verify(groupBookmarkRepository).deleteByMemberIdAndGroupId(10L, 1L)
        verify(notificationService).createNotification(
            userId = 10L,
            category = NotificationCategory.GROUP,
            title = "그룹 즐겨찾기 해제",
            content = "‘테스트 그룹’ 그룹을 즐겨찾기에서 해제했습니다.",
        )
    }

    @Test
    fun `cancelBookmark는 즐겨찾기하지 않은 그룹에 요청하면 422를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupBookmarkRepository.deleteByMemberIdAndGroupId(10L, 1L)).willReturn(0)

        // when & then
        assertThrows<UnprocessableEntityException> {
            groupService.cancelBookmark(groupId = 1L, requesterId = 10L)
        }
        verifyNoInteractions(notificationService)
    }

    @Test
    fun `cancelBookmark는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            groupService.cancelBookmark(groupId = 1L, requesterId = 10L)
        }
        verify(groupBookmarkRepository, never()).deleteByMemberIdAndGroupId(any(), any())
        verifyNoInteractions(notificationService)
    }

    @Test
    fun `중단된 그룹에서는 즐겨찾기를 삭제하거나 해제 알림을 만들지 않는다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(testGroup(status = GroupStatus.STOP)))

        assertThrows<BadRequestException> {
            groupService.cancelBookmark(groupId = 1L, requesterId = 10L)
        }

        verify(groupBookmarkRepository, never()).deleteByMemberIdAndGroupId(any(), any())
        verifyNoInteractions(notificationService)
    }

    @Test
    fun `publishPortfolio는 그룹장이 미발행 그룹에 요청하면 포트폴리오 발행 여부를 true로 변경하고 멤버에게 포인트와 알림을 전송한다`() {
        // given
        val members = listOf(testGroupMember(groupId = 1L, memberId = 20L), testGroupMember(groupId = 1L, memberId = 30L))
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(members)

        // when
        val result = groupService.publishPortfolio(groupId = 1L, requesterId = 10L)

        // then
        assertTrue(result)
        verify(groupRepository).updatePortfolioPublishedById(1L, true, GroupStatus.PROGRESS, 10L)
        members.forEach { member ->
            verify(memberPointRepository).addPoints(member.memberId, 10)
            verify(memberPointRepository).recordPointChange(member.memberId, 10L, 10, "포트폴리오 발행")
            verify(notificationService).createNotification(
                userId = member.memberId,
                category = NotificationCategory.GROUP,
                title = "포트폴리오 발행",
                content = "테스트 그룹 그룹의 포트폴리오가 발행되었습니다.",
            )
        }
        verify(groupLogService).createLog(1L, 10L, GroupLogData.GroupPortfolioPublished)
    }

    @Test
    fun `publishPortfolio는 이미 발행된 그룹에 요청하면 400을 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(testGroup(id = 1L, leaderMemberId = 10L, isPortfolioPublished = true)))

        // when & then
        assertThrows<BadRequestException> {
            groupService.publishPortfolio(groupId = 1L, requesterId = 10L)
        }
    }

    @Test
    fun `publishPortfolio는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            groupService.publishPortfolio(groupId = 1L, requesterId = 10L)
        }
    }

    @Test
    fun `cancelPortfolio는 그룹장이 발행 중인 그룹에 요청하면 포트폴리오 발행 여부를 false로 변경하고 멤버에게 포인트와 알림을 전송한다`() {
        // given
        val publishedGroup = testGroup(id = 1L, leaderMemberId = 10L, isPortfolioPublished = true)
        val members = listOf(testGroupMember(groupId = 1L, memberId = 20L), testGroupMember(groupId = 1L, memberId = 30L))
        given(groupRepository.findById(1L)).willReturn(Optional.of(publishedGroup))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(members)

        // when
        groupService.cancelPortfolio(groupId = 1L, requesterId = 10L)

        // then
        verify(groupRepository).updatePortfolioPublishedById(1L, false, GroupStatus.PROGRESS, 10L)
        members.forEach { member ->
            verify(memberPointRepository).addPoints(member.memberId, -10)
            verify(memberPointRepository).recordPointChange(member.memberId, 10L, -10, "포트폴리오 취소")
            verify(notificationService).createNotification(
                userId = member.memberId,
                category = NotificationCategory.GROUP,
                title = "포트폴리오 취소",
                content = "테스트 그룹 그룹의 포트폴리오가 취소되었습니다.",
            )
        }
        verify(groupLogService).createLog(1L, 10L, GroupLogData.GroupPortfolioUnpublished)
    }

    @Test
    fun `cancelPortfolio는 발행되지 않은 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))

        // when & then
        assertThrows<NotFoundException> {
            groupService.cancelPortfolio(groupId = 1L, requesterId = 10L)
        }
    }

    @Test
    fun `cancelPortfolio는 존재하지 않는 그룹에 요청하면 404를 반환한다`() {
        // given
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            groupService.cancelPortfolio(groupId = 1L, requesterId = 10L)
        }
    }

    @ParameterizedTest
    @EnumSource(value = GroupStatus::class, names = ["STOP", "SUCCESS", "FAIL"])
    fun `중단되거나 종료된 그룹은 포트폴리오를 변경할 수 없다`(status: GroupStatus) {
        given(groupRepository.findById(1L)).willReturn(Optional.of(testGroup(leaderMemberId = 10L, status = status)))

        assertThrows<BadRequestException> { groupService.publishPortfolio(1L, 10L) }
        assertThrows<BadRequestException> { groupService.cancelPortfolio(1L, 10L) }

        verify(groupRepository, never()).updatePortfolioPublishedById(any(), any(), any(), any())
        verifyNoInteractions(memberPointRepository, notificationService, groupLogService)
    }

    @Test
    fun `다른 요청이 이미 포트폴리오를 발행하거나 취소했으면 부수 효과를 남기지 않는다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.of(baseGroup))
        given(groupRepository.updatePortfolioPublishedById(1L, true, GroupStatus.PROGRESS, 10L)).willReturn(0)

        assertThrows<BadRequestException> { groupService.publishPortfolio(1L, 10L) }

        given(groupRepository.findById(1L))
            .willReturn(Optional.of(testGroup(leaderMemberId = 10L, isPortfolioPublished = true)))
        given(groupRepository.updatePortfolioPublishedById(1L, false, GroupStatus.PROGRESS, 10L)).willReturn(0)

        assertThrows<NotFoundException> { groupService.cancelPortfolio(1L, 10L) }
        verifyNoInteractions(memberPointRepository, notificationService, groupLogService)
    }
}
