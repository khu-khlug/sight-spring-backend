package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

class UserServiceTest {
    private val discordIntegrationRepository: DiscordIntegrationRepository = mock()
    private val discordMemberService: DiscordMemberService = mock()
    private val memberRepository: MemberRepository = mock()
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService =
            UserService(
                discordIntegrationRepository = discordIntegrationRepository,
                discordMemberService = discordMemberService,
                memberRepository = memberRepository,
            )
    }

    @Test
    fun `getMemberById는 존재하는 사용자를 반환한다`() {
        // given
        val userId = 1L
        val member =
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
                lastLogin = LocalDateTime.now(),
                lastEnter = LocalDateTime.now(),
            )
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
        val exception =
            assertThrows<NotFoundException> {
                userService.getMemberById(userId)
            }
    }
}
