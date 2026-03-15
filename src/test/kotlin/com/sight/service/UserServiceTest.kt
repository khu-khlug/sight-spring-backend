package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.FeeHistoryRepository
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
import java.time.temporal.ChronoUnit
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserServiceTest {
    private val discordIntegrationRepository: DiscordIntegrationRepository = mock()
    private val discordMemberService: DiscordMemberService = mock()
    private val memberRepository: MemberRepository = mock()
    private val feeHistoryRepository: FeeHistoryRepository = mock()
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
                feeHistoryRepository = feeHistoryRepository,
                pointService = pointService,
                notificationService = notificationService,
            )
    }

    @Test
    fun `getMemberById는 존재하는 사용자를 반환한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now())
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
        val member = createMember(userId, lastLogin = Instant.now().minus(1, ChronoUnit.DAYS))
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))
        val captor = argumentCaptor<Member>()

        // when
        userService.checkFirstTodayLogin(userId)

        // then
        verify(memberRepository).save(captor.capture())
        assertTrue(
            captor.firstValue.lastLogin
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDate() ==
                Instant.now().atZone(ZoneId.of("Asia/Seoul")).toLocalDate(),
        )
    }

    @Test
    fun `checkFirstTodayLogin은 오늘 첫 방문이면 포인트를 지급한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now().minus(1, ChronoUnit.DAYS))
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        userService.checkFirstTodayLogin(userId)

        // then
        verify(pointService).givePoint(
            targetUserId = eq(userId),
            point = eq(1),
            message = any(),
        )
    }

    @Test
    fun `checkFirstTodayLogin은 오늘 이미 방문한 경우 포인트 지급을 하지 않는다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now())
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        userService.checkFirstTodayLogin(userId)

        // then
        verify(pointService, never()).givePoint(any(), any(), any())
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

    @Test
    fun `graduateMember는 존재하지 않는 사용자일 때 NotFoundException을 발생시킨다`() {
        // given
        val userId = 999L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            userService.graduateMember(userId)
        }
    }

    @Test
    fun `graduateMember는 이미 졸업한 사용자일 때 UnprocessableEntityException을 발생시킨다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now()).copy(studentStatus = StudentStatus.GRADUATE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when & then
        assertThrows<UnprocessableEntityException> {
            userService.graduateMember(userId)
        }
    }

    @Test
    fun `graduateMember는 정상적으로 졸업 처리한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now())
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))
        val captor = argumentCaptor<Member>()

        // when
        userService.graduateMember(userId)

        // then
        verify(memberRepository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals(StudentStatus.GRADUATE, saved.studentStatus)
        assertEquals(0L, saved.grade)
        assertFalse(saved.manager)
    }

    @Test
    fun `graduateMember는 졸업 처리 후 Discord 역할을 갱신한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now())
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        userService.graduateMember(userId)

        // then
        verify(discordMemberService).reflectUserInfoToDiscordUser(userId)
    }

    @Test
    fun `ungraduateMember는 존재하지 않는 사용자일 때 NotFoundException을 발생시킨다`() {
        // given
        val userId = 999L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            userService.ungraduateMember(userId)
        }
    }

    @Test
    fun `ungraduateMember는 이미 재학 중인 사용자일 때 UnprocessableEntityException을 발생시킨다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now()).copy(studentStatus = StudentStatus.UNDERGRADUATE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when & then
        assertThrows<UnprocessableEntityException> {
            userService.ungraduateMember(userId)
        }
    }

    @Test
    fun `ungraduateMember는 정상적으로 졸업 취소 처리한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now()).copy(studentStatus = StudentStatus.GRADUATE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))
        val captor = argumentCaptor<Member>()

        // when
        userService.ungraduateMember(userId)

        // then
        verify(memberRepository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals(StudentStatus.UNDERGRADUATE, saved.studentStatus)
        assertEquals(4L, saved.grade)
    }

    @Test
    fun `ungraduateMember는 졸업 취소 후 Discord 역할을 갱신한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now()).copy(studentStatus = StudentStatus.GRADUATE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        userService.ungraduateMember(userId)

        // then
        verify(discordMemberService).reflectUserInfoToDiscordUser(userId)
    }

    @Test
    fun `deleteMember는 존재하지 않는 사용자일 때 NotFoundException을 발생시킨다`() {
        // given
        val userId = 999L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            userService.deleteMember(userId)
        }
    }

    @Test
    fun `deleteMember는 정상적으로 탈퇴 처리한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now())
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))
        val captor = argumentCaptor<Member>()

        // when
        userService.deleteMember(userId)

        // then
        verify(memberRepository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals(UserStatus.UNAUTHORIZED, saved.status)
        assertEquals("", saved.password)
        assertEquals(0L, saved.number)
        assertEquals("", saved.email)
        assertEquals("", saved.phone)
        assertEquals("", saved.homepage)
        assertEquals("", saved.language)
        assertEquals("", saved.prefer)
        assertEquals(0L, saved.expoint)
        assertFalse(saved.manager)
        assertNull(saved.slack)
        assertNull(saved.returnAt)
        assertNull(saved.returnReason)
    }

    @Test
    fun `deleteMember는 탈퇴 후 Discord 연동을 제거한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, lastLogin = Instant.now())
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        userService.deleteMember(userId)

        // then
        verify(discordMemberService).clearDiscordIntegration(userId)
    }

    @Test
    fun `listMembers는 필터 조건에 맞는 회원 목록과 총 개수를 반환한다`() {
        // given
        val members =
            listOf(
                createMember(1L, lastLogin = Instant.now()),
                createMember(2L, lastLogin = Instant.now()),
            )
        whenever(memberRepository.findMembers(null, null, null, null, null, null, null, null, 10, 0))
            .thenReturn(members)
        whenever(memberRepository.countMembers(null, null, null, null, null, null, null, null))
            .thenReturn(2L)
        whenever(feeHistoryRepository.findByUserIdInAndYearAndSemester(any(), any(), any()))
            .thenReturn(emptyList())

        // when
        val (count, result) =
            userService.listMembers(
                email = null,
                phone = null,
                name = null,
                number = null,
                college = null,
                grade = null,
                studentStatus = null,
                tag = null,
                limit = 10,
                offset = 0,
            )

        // then
        assertEquals(2L, count)
        assertEquals(2, result.size)
    }

    // appointManager
    @Test
    fun `appointManager는 요청자와 운영진에 임명하려는 유저가 동일하면 예외를 발생시켜야 한다`() {
        val requesterUserId = 123L
        val targetUserId = requesterUserId

        assertThrows<UnprocessableEntityException> {
            userService.appointManager(requesterUserId, targetUserId)
        }
    }

    @Test
    fun `appointManager는 대상 유저가 존재하지 않으면 예외를 발생시켜야 한다`() {
        val requesterUserId = 100L
        val targetUserId = 200L

        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> {
            userService.appointManager(requesterUserId, targetUserId)
        }
    }

    @Test
    fun `appointManager는 대상 유저가 인증되지 않은 상태라면 예외를 발생시켜야 한다`() {
        val requesterUserId = 100L
        val targetUserId = 200L

        val unauthorizedMember = createMember(status = UserStatus.UNAUTHORIZED)
        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.of(unauthorizedMember))

        assertThrows<UnprocessableEntityException> {
            userService.appointManager(requesterUserId, targetUserId)
        }
    }

    @Test
    fun `appointManager는 이미 대상 유저가 운영진이라면 별도 저장 없이 종료되어야 한다`() {
        val requesterUserId = 100L
        val targetUserId = 200L

        val managerMember = createMember(manager = true)
        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.of(managerMember))

        userService.appointManager(requesterUserId, targetUserId)

        verify(memberRepository, never()).save(any())
    }

    @Test
    fun `appointManager는 대상 유저를 운영진으로 임명해야 한다`() {
        val requesterUserId = 100L
        val targetUserId = 200L

        val member = createMember(status = UserStatus.ACTIVE, manager = false)
        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.of(member))

        userService.appointManager(requesterUserId, targetUserId)

        val captor = argumentCaptor<Member>()
        verify(memberRepository).save(captor.capture())
        assertTrue(captor.firstValue.manager)
    }

    // stepdownManager
    @Test
    fun `stepdownManager는 요청자와 운영진에서 퇴임시키려는 유저가 동일하면 예외를 발생시켜야 한다`() {
        val requesterUserId = 123L
        val targetUserId = requesterUserId

        assertThrows<UnprocessableEntityException> {
            userService.stepdownManager(requesterUserId, targetUserId)
        }
    }

    @Test
    fun `stepdownManager는 대상 유저가 존재하지 않으면 예외를 발생시켜야 한다`() {
        val requesterUserId = 100L
        val targetUserId = 200L

        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> {
            userService.stepdownManager(requesterUserId, targetUserId)
        }
    }

    @Test
    fun `stepdownManager는 대상 유저가 운영진이 아니라면 별도 저장 없이 종료되어야 한다`() {
        val requesterUserId = 100L
        val targetUserId = 200L

        val notManagerMember = createMember(manager = false)
        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.of(notManagerMember))

        userService.stepdownManager(requesterUserId, targetUserId)

        verify(memberRepository, never()).save(any())
    }

    @Test
    fun `stepdownManager는 대상 유저를 운영진에서 퇴임시켜야 한다`() {
        val requesterUserId = 100L
        val targetUserId = 200L

        val member = createMember(manager = true)
        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.of(member))

        userService.stepdownManager(requesterUserId, targetUserId)

        val captor = argumentCaptor<Member>()
        verify(memberRepository).save(captor.capture())
        assertFalse(captor.firstValue.manager)
    }

    // expelMember
    @Test
    fun `expelMember는 자기 자신을 제명하려고 하면 BadRequestException을 발생시킨다`() {
        // given
        val userId = 1L

        // when & then
        assertThrows<BadRequestException> {
            userService.expelMember(requesterUserId = userId, targetUserId = userId)
        }
    }

    @Test
    fun `expelMember는 존재하지 않는 사용자일 때 NotFoundException을 발생시킨다`() {
        // given
        val requesterUserId = 1L
        val targetUserId = 999L
        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            userService.expelMember(requesterUserId = requesterUserId, targetUserId = targetUserId)
        }
    }

    @Test
    fun `expelMember는 이미 탈퇴한 회원이면 BadRequestException을 발생시킨다`() {
        // given
        val requesterUserId = 1L
        val targetUserId = 2L
        val member = createMember(targetUserId, status = UserStatus.UNAUTHORIZED)
        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.of(member))

        // when & then
        assertThrows<BadRequestException> {
            userService.expelMember(requesterUserId = requesterUserId, targetUserId = targetUserId)
        }
    }

    @Test
    fun `expelMember는 정상적으로 회원을 제명 처리한다`() {
        // given
        val requesterUserId = 1L
        val targetUserId = 2L
        val member = createMember(targetUserId, status = UserStatus.ACTIVE)
        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.of(member))
        val captor = argumentCaptor<Member>()

        // when
        userService.expelMember(requesterUserId = requesterUserId, targetUserId = targetUserId)

        // then
        verify(memberRepository).save(captor.capture())
        val saved = captor.firstValue
        assertEquals(targetUserId.toString(), saved.name)
        assertEquals("", saved.password)
        assertEquals("", saved.email)
        assertEquals("", saved.phone)
        assertEquals("", saved.homepage)
        assertEquals("", saved.language)
        assertEquals("", saved.prefer)
        assertEquals(0L, saved.number)
        assertEquals(0L, saved.expoint)
        assertEquals(UserStatus.UNAUTHORIZED, saved.status)
        assertFalse(saved.manager)
        assertNull(saved.slack)
        assertNull(saved.returnAt)
        assertNull(saved.returnReason)
    }

    @Test
    fun `expelMember는 제명 후 Discord 연동을 제거한다`() {
        // given
        val requesterUserId = 1L
        val targetUserId = 2L
        val member = createMember(targetUserId, status = UserStatus.ACTIVE)
        whenever(memberRepository.findById(targetUserId)).thenReturn(Optional.of(member))

        // when
        userService.expelMember(requesterUserId = requesterUserId, targetUserId = targetUserId)

        // then
        verify(discordMemberService).clearDiscordIntegration(targetUserId)
    }

    // blockMember
    @Test
    fun `blockMember는 존재하지 않는 사용자일 때 NotFoundException을 발생시킨다`() {
        // given
        val userId = 999L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            userService.blockMember(userId)
        }
    }

    @Test
    fun `blockMember는 ACTIVE 상태가 아닌 회원이면 UnprocessableEntityException을 발생시킨다`() {
        // given
        val userId = 1L
        val member = createMember(userId, status = UserStatus.INACTIVE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when & then
        assertThrows<UnprocessableEntityException> {
            userService.blockMember(userId)
        }
    }

    @Test
    fun `blockMember는 ACTIVE 회원의 상태를 INACTIVE로 변경한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, status = UserStatus.ACTIVE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))
        val captor = argumentCaptor<Member>()

        // when
        userService.blockMember(userId)

        // then
        verify(memberRepository).save(captor.capture())
        assertEquals(UserStatus.INACTIVE, captor.firstValue.status)
    }

    @Test
    fun `blockMember는 차단 시 포인트를 차감한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, status = UserStatus.ACTIVE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        userService.blockMember(userId)

        // then
        verify(pointService).givePoint(
            targetUserId = eq(userId),
            point = eq(-1100),
            message = eq("사이트 접속이 차단되었습니다."),
        )
    }

    // unblockMember
    @Test
    fun `unblockMember는 존재하지 않는 사용자일 때 NotFoundException을 발생시킨다`() {
        // given
        val userId = 999L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            userService.unblockMember(userId)
        }
    }

    @Test
    fun `unblockMember는 INACTIVE 상태가 아닌 회원이면 UnprocessableEntityException을 발생시킨다`() {
        // given
        val userId = 1L
        val member = createMember(userId, status = UserStatus.ACTIVE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when & then
        assertThrows<UnprocessableEntityException> {
            userService.unblockMember(userId)
        }
    }

    @Test
    fun `unblockMember는 INACTIVE 회원의 상태를 ACTIVE로 변경한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, status = UserStatus.INACTIVE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))
        val captor = argumentCaptor<Member>()

        // when
        userService.unblockMember(userId)

        // then
        verify(memberRepository).save(captor.capture())
        assertEquals(UserStatus.ACTIVE, captor.firstValue.status)
    }

    @Test
    fun `unblockMember는 차단 해제 시 포인트를 지급한다`() {
        // given
        val userId = 1L
        val member = createMember(userId, status = UserStatus.INACTIVE)
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(member))

        // when
        userService.unblockMember(userId)

        // then
        verify(pointService).givePoint(
            targetUserId = eq(userId),
            point = eq(700),
            message = eq("사이트 접속 차단이 해제되었습니다."),
        )
    }

    private fun createMember(
        userId: Long = 123L,
        lastLogin: Instant = Instant.now(),
        manager: Boolean = false,
        status: UserStatus = UserStatus.ACTIVE,
    ): Member =
        Member(
            id = userId,
            name = "testuser",
            admission = "20",
            realname = "테스트 사용자",
            college = "소프트웨어융합학과",
            grade = 3L,
            manager = manager,
            studentStatus = StudentStatus.UNDERGRADUATE,
            email = "test@example.com",
            status = status,
            khuisauthAt = Instant.now(),
            updatedAt = LocalDateTime.now(),
            createdAt = Instant.now(),
            lastLogin = lastLogin,
            lastEnter = LocalDateTime.now(),
        )
}
