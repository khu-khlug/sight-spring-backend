package com.sight.service

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupInterest
import com.sight.domain.group.GroupLogData
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
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupServiceUpdateGroupTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val notificationService = mock<NotificationService>()
    private val groupLogService = mock<GroupLogService>()
    private val groupService =
        GroupService(
            integerIdService = mock<IntegerIdService>(),
            groupRepository = groupRepository,
            groupBookmarkRepository = mock<GroupBookmarkRepository>(),
            groupMemberRepository = groupMemberRepository,
            pointService = mock<PointService>(),
            notificationService = notificationService,
            groupLogService = groupLogService,
        )

    private val leader = Requester(userId = 10L, role = UserRole.USER)
    private val manager = Requester(userId = 20L, role = UserRole.MANAGER)

    private fun stubGroup(
        category: GroupCategory = GroupCategory.STUDY,
        status: GroupStatus = GroupStatus.PROGRESS,
        visibility: GroupVisibility = GroupVisibility.MEMBER_ONLY,
    ) = testGroup(
        id = 1L,
        title = "기존 그룹",
        category = category,
        leaderMemberId = 10L,
        status = status,
        visibility = visibility,
        allowJoin = true,
    ).also {
        given(groupRepository.findById(1L)).willReturn(Optional.of(it))
        given(groupMemberRepository.findByGroupId(1L)).willReturn(
            listOf(testGroupMember(groupId = 1L, memberId = 10L), testGroupMember(groupId = 1L, memberId = 11L)),
        )
    }

    private fun update(
        requester: Requester = leader,
        title: String = "새 그룹",
        description: String? = "설명",
        category: GroupCategory = GroupCategory.STUDY,
        interests: List<GroupInterest>? = listOf(GroupInterest.WEB_APP_SERVICE),
        techStack: String? = "react,typescript",
        repositoryUrls: String? = "https://github.com/a/b",
        allowJoin: Boolean = false,
        visibility: GroupVisibility = GroupVisibility.PUBLIC,
    ) = groupService.updateGroup(
        groupId = 1L,
        requester = requester,
        title = title,
        description = description,
        category = category,
        interests = interests,
        techStack = techStack,
        repositoryUrls = repositoryUrls,
        allowJoin = allowJoin,
        visibility = visibility,
    )

    private fun verifyNoSideEffects() {
        verifyNoInteractions(groupLogService, notificationService)
    }

    @Test
    fun `그룹장은 모든 설정 값을 요청 값으로 덮어쓰고 변경 항목을 로그와 알림에 남긴다`() {
        val group = stubGroup()
        val lastActivityAt = group.lastActivityAt

        update()

        assertEquals("새 그룹", group.title)
        assertEquals("설명", group.description)
        assertEquals(listOf(GroupInterest.WEB_APP_SERVICE), group.interests)
        assertEquals("react,typescript", group.techStack)
        assertEquals("https://github.com/a/b", group.repositoryUrls)
        assertEquals(false, group.allowJoin)
        assertEquals(GroupVisibility.PUBLIC, group.visibility)
        // 정보 변경은 활동이 아니므로 최근 활동 일시와 상태를 바꾸지 않는다.
        assertEquals(lastActivityAt, group.lastActivityAt)
        assertEquals(GroupStatus.PROGRESS, group.status)
        verify(groupRepository, never()).recordActivityById(any(), any())
        verify(groupRepository, never()).resumeStoppedById(any(), any(), any())

        val expectedFields = listOf("이름", "설명", "관심 분야", "사용 기술", "저장소", "참여 신청 가능 여부", "공개 범위")
        verify(groupLogService).createLog(
            eq(1L),
            eq(10L),
            eq(GroupLogData.GroupInfoUpdated(changedFields = expectedFields)),
        )
        // 그룹원 전체에게 알린다.
        listOf(10L, 11L).forEach { memberId ->
            verify(notificationService).createNotification(
                eq(memberId),
                eq(NotificationCategory.GROUP),
                eq(""),
                argThat { contains("새 그룹") && contains("변경 항목: 이름, 설명") },
            )
        }
    }

    @Test
    fun `운영진은 운영 그룹의 정보를 운영진만 공개로 바꿀 수 있다`() {
        val group = stubGroup(category = GroupCategory.MANAGE, visibility = GroupVisibility.MEMBER_ONLY)

        update(
            requester = manager,
            category = GroupCategory.MANAGE,
            visibility = GroupVisibility.MANAGER_ONLY,
        )

        assertEquals(GroupVisibility.MANAGER_ONLY, group.visibility)
    }

    @Test
    fun `운영진이 아니면 운영진 전용 유형을 지정할 수 없다`() {
        stubGroup()

        listOf(GroupCategory.MANAGE, GroupCategory.EDUCATION, GroupCategory.PROGRAM).forEach { category ->
            assertThrows<ForbiddenException> { update(category = category) }
        }
        verifyNoSideEffects()
    }

    @Test
    fun `운영진이 아니면 운영진만 공개를 지정할 수 없다`() {
        stubGroup(category = GroupCategory.MANAGE)

        assertThrows<ForbiddenException> { update(visibility = GroupVisibility.MANAGER_ONLY) }
        verifyNoSideEffects()
    }

    @Test
    fun `운영진만 공개는 운영 유형이 아니면 운영진이어도 400이다`() {
        stubGroup()

        assertThrows<BadRequestException> {
            update(requester = manager, category = GroupCategory.STUDY, visibility = GroupVisibility.MANAGER_ONLY)
        }
        verifyNoSideEffects()
    }

    @Test
    fun `그룹 이름이 공백이거나 255자를 넘으면 400이고 255자는 저장한다`() {
        val group = stubGroup()

        assertThrows<BadRequestException> { update(title = "   ") }
        assertThrows<BadRequestException> { update(title = "가".repeat(256)) }
        update(title = "가".repeat(255))

        assertEquals("가".repeat(255), group.title)
    }

    @Test
    fun `사용 기술과 저장소는 각각 합친 문자열이 255자를 넘으면 400이다`() {
        stubGroup()
        val longTech = "a".repeat(256)
        val longUrls = "https://" + "a".repeat(248)

        assertThrows<BadRequestException> { update(techStack = longTech) }
        assertThrows<BadRequestException> { update(repositoryUrls = longUrls) }

        // 255자는 허용한다.
        update(techStack = "a".repeat(255), repositoryUrls = "https://" + "a".repeat(247))
    }

    @Test
    fun `빈 문자열이나 null인 사용 기술과 저장소는 없음으로 저장한다`() {
        val group = stubGroup()

        update(techStack = "", repositoryUrls = "")
        assertNull(group.techStack)
        assertNull(group.repositoryUrls)

        update(techStack = "kotlin", repositoryUrls = "https://a.com")
        update(techStack = null, repositoryUrls = null)
        assertNull(group.techStack)
        assertNull(group.repositoryUrls)
    }

    @Test
    fun `저장소는 5개까지 저장하고 6개나 중복·빈 항목·http 외 스킴·CR은 400이다`() {
        val group = stubGroup()
        val five = (1..5).joinToString("\n") { "https://a.com/$it" }

        update(repositoryUrls = five)
        assertEquals(five, group.repositoryUrls)

        assertThrows<BadRequestException> { update(repositoryUrls = (1..6).joinToString("\n") { "https://a.com/$it" }) }
        assertThrows<BadRequestException> { update(repositoryUrls = "https://a.com\nhttps://a.com") }
        assertThrows<BadRequestException> { update(repositoryUrls = "https://a.com\n\nhttps://b.com") }
        assertThrows<BadRequestException> { update(repositoryUrls = "javascript:alert(1)") }
        assertThrows<BadRequestException> { update(repositoryUrls = "data:text/html,hi") }
        assertThrows<BadRequestException> { update(repositoryUrls = "https://a.com/a\rb") }
    }

    @Test
    fun `저장소 URL의 쉼표와 퍼센트 인코딩은 디코딩하지 않고 그대로 둔다`() {
        val group = stubGroup()
        val urls = "https://a.com/x,y?q=%0A%2C\nhttps://b.com"

        update(repositoryUrls = urls)

        assertEquals(urls, group.repositoryUrls)
    }

    @Test
    fun `사용 기술과 관심 분야에 중복이나 빈 이름이 있으면 400이다`() {
        stubGroup()

        assertThrows<BadRequestException> { update(techStack = "react,react") }
        assertThrows<BadRequestException> { update(techStack = "react,,vue") }
        assertThrows<BadRequestException> {
            update(interests = listOf(GroupInterest.UX_UI_DESIGN, GroupInterest.UX_UI_DESIGN))
        }
    }

    @Test
    fun `이름과 사용 기술의 제어 문자는 400이고 설명은 줄바꿈만 허용한다`() {
        val group = stubGroup()

        assertThrows<BadRequestException> { update(title = "가\n나") }
        assertThrows<BadRequestException> { update(techStack = "react,type\nscript") }
        assertThrows<BadRequestException> { update(description = "탭\t포함") }
        update(description = "첫 줄\n둘째 줄")

        assertEquals("첫 줄\n둘째 줄", group.description)
    }

    @Test
    fun `HTML은 해석하거나 바꾸지 않고 그대로 저장한다`() {
        val group = stubGroup()

        update(title = "<b>그룹</b>", description = "<script>alert(1)</script>")

        assertEquals("<b>그룹</b>", group.title)
        assertEquals("<script>alert(1)</script>", group.description)
    }

    @Test
    fun `현재와 같은 값으로 요청하면 아무것도 바꾸지 않고 알림과 로그를 남기지 않는다`() {
        val group = stubGroup()
        update()
        org.mockito.kotlin.clearInvocations(groupLogService, notificationService)

        update()

        assertEquals("새 그룹", group.title)
        verifyNoSideEffects()
    }

    @Test
    fun `일부 항목만 바뀌면 바뀐 항목만 알린다`() {
        stubGroup()
        update()
        org.mockito.kotlin.clearInvocations(groupLogService, notificationService)

        update(title = "다른 이름")

        verify(groupLogService).createLog(
            eq(1L),
            eq(10L),
            eq(GroupLogData.GroupInfoUpdated(changedFields = listOf("이름"))),
        )
        verify(notificationService, times(2)).createNotification(any(), any(), any(), argThat { contains("변경 항목: 이름)") })
    }

    @Test
    fun `중단되거나 종료된 그룹은 400이고 아무것도 바꾸지 않는다`() {
        listOf(GroupStatus.STOP, GroupStatus.SUCCESS, GroupStatus.FAIL).forEach { status ->
            val group = stubGroup(status = status)

            assertThrows<BadRequestException> { update() }

            assertEquals("기존 그룹", group.title)
            assertEquals(status, group.status)
        }
        verifyNoSideEffects()
    }

    @Test
    fun `그룹이 없으면 404이다`() {
        given(groupRepository.findById(1L)).willReturn(Optional.empty())

        assertThrows<NotFoundException> { update() }
        verifyNoSideEffects()
    }

    @Test
    fun `거부된 요청은 최근 활동 일시를 바꾸지 않는다`() {
        val group = stubGroup()
        val lastActivityAt: Instant = group.lastActivityAt

        assertThrows<BadRequestException> { update(title = " ") }

        assertEquals(lastActivityAt, group.lastActivityAt)
        verify(groupRepository, never()).recordActivityById(any(), any())
    }
}
