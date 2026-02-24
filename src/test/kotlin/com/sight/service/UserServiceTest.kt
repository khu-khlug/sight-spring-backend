package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserServiceTest {
    private val discordIntegrationRepository: DiscordIntegrationRepository = mock()
    private val discordMemberService: DiscordMemberService = mock()
    private val memberRepository: MemberRepository = mock()
    private val pointService: PointService = mock()
    private val notificationService: NotificationService = mock()
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService =
            UserService(
                discordIntegrationRepository = discordIntegrationRepository,
                discordMemberService = discordMemberService,
                memberRepository = memberRepository,
                pointService = pointService,
                notificationService = notificationService,
            )
    }

    @Test
    fun `getMemberById는 존재하는 사용자를 반환한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = LocalDateTime.now())
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        val result = userService.getMemberById(userId)

        // then
        assertEquals(member, result)
    }

    @Test
    fun `getMemberById는 존재하지 않는 사용자일 때 NotFoundException을 발생시킨다`() {
        // given
        val userId = 999L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            userService.getMemberById(userId)
        }
    }

    @Test
    fun `checkFirstTodayLogin은 방문 여부와 관계없이 lastLogin을 갱신한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = LocalDateTime.now().minusDays(1))
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))
        val captor = argumentCaptor<Member>()

        // when
        userService.checkFirstTodayLogin(userId)

        // then
        verify(memberRepository).save(captor.capture())
        val kst = ZoneId.of("Asia/Seoul")
        val todayKst = Instant.now().atZone(kst).toLocalDate()
        assertTrue(captor.firstValue.lastLogin.atZone(kst).toLocalDate() == todayKst)
    }

    @Test
    fun `checkFirstTodayLogin은 오늘 첫 방문이면 포인트를 지급하고 알림을 생성한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = LocalDateTime.now().minusDays(1))
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        userService.checkFirstTodayLogin(userId)

        // then
        verify(pointService).givePoint(
            targetUserId = eq(userId),
            point = eq(1),
            message = any(),
        )
        verify(notificationService).createNotification(
            userId = eq(userId),
            category = eq(NotificationCategory.SYSTEM),
            title = eq("일일 첫 방문"),
            content = eq("오늘의 첫 방문을 축하합니다! 포인트 1점이 지급되었습니다."),
            url = eq(null),
        )
    }

    @Test
    fun `checkFirstTodayLogin은 오늘 이미 방문한 경우 포인트 지급과 알림 생성을 하지 않는다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = LocalDateTime.now())
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        userService.checkFirstTodayLogin(userId)

        // then
        verify(pointService, never()).givePoint(any(), any(), any())
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any())
    }

    @Test
    fun `checkFirstTodayLogin은 사용자가 존재하지 않으면 NotFoundException을 발생시킨다`() {
        // given
        val userId = 999L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            userService.checkFirstTodayLogin(userId)
        }
    }

    private fun createMember(
        userId: Long,
        lastLogin: LocalDateTime,
    ): Member =
        Member(
            id = userId,
            name = "testuser",
            admission = "20",
            realname = "테스트 사용자",
            college = "소프트웨어융합학과",
            grade = 3L,
            studentStatus = StudentStatus.UNDERGRADUATE,
            email = "test@example.com",
            status = UserStatus.ACTIVE,
            khuisauthAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            lastLogin = lastLogin,
            lastEnter = LocalDateTime.now(),
        )
}
