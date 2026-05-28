package com.sight.service

import com.sight.core.exception.ConflictException
import com.sight.core.exception.UnauthorizedException
import com.sight.core.khuis.KhuisClient
import com.sight.domain.application.UserRegistrationRequest
import com.sight.domain.application.UserRegistrationRequestStatus
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.MemberRepository
import com.sight.repository.UserRegistrationRequestRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserRegistrationRequestServiceTest {
    private val memberRepository = mock<MemberRepository>()
    private val userRegistrationRequestRepository = mock<UserRegistrationRequestRepository>()
    private val khuisClient = mock<KhuisClient>()
    private lateinit var userRegistrationRequestService: UserRegistrationRequestService

    @BeforeEach
    fun setUp() {
        userRegistrationRequestService =
            UserRegistrationRequestService(
                memberRepository,
                userRegistrationRequestRepository,
                khuisClient,
            )
    }

    private fun createMember(
        id: Long = 1L,
        name: String = "testuser",
        status: UserStatus = UserStatus.UNAUTHORIZED,
    ) = Member(
        id = id,
        name = name,
        realname = "홍길동",
        studentStatus = StudentStatus.UNDERGRADUATE,
        status = status,
        createdAt = Instant.now(),
        updatedAt = java.time.LocalDateTime.now(),
        lastLogin = Instant.now(),
        lastEnter = java.time.LocalDateTime.now(),
    )

    @Test
    fun `createRegistrationRequest는 모든 검증을 통과하면 정상 신청을 저장하고 반환한다`() {
        // given
        val info21Id = "khu123"
        val info21Password = "pass123!"
        val requestedUserId = 100L

        val member = createMember(id = requestedUserId, name = info21Id, status = UserStatus.UNAUTHORIZED)
        val savedRequest =
            UserRegistrationRequest(
                id = "req-ulid",
                requestedUserId = requestedUserId,
                status = UserRegistrationRequestStatus.PENDING,
            )

        given(memberRepository.findByName(info21Id)).willReturn(member)
        given(userRegistrationRequestRepository.existsByRequestedUserIdAndStatus(requestedUserId, UserRegistrationRequestStatus.PENDING))
            .willReturn(false)
        given(khuisClient.authenticate(info21Id, info21Password)).willReturn(true)
        given(userRegistrationRequestRepository.save(any<UserRegistrationRequest>())).willReturn(savedRequest)

        // when
        val result =
            userRegistrationRequestService.createRegistrationRequest(
                info21Id = info21Id,
                info21Password = info21Password,
                requestedUserId = requestedUserId,
            )

        // then
        assertNotNull(result)
        assertEquals("req-ulid", result.id)
        assertEquals(requestedUserId, result.requestedUserId)
        assertEquals(UserRegistrationRequestStatus.PENDING, result.status)

        verify(memberRepository).findByName(info21Id)
        verify(userRegistrationRequestRepository).existsByRequestedUserIdAndStatus(requestedUserId, UserRegistrationRequestStatus.PENDING)
        verify(khuisClient).authenticate(info21Id, info21Password)
        verify(userRegistrationRequestRepository).save(any<UserRegistrationRequest>())
    }

    @Test
    fun `createRegistrationRequest는 이미 가입 완료된 회원인 경우 ConflictException을 발생시킨다`() {
        // given
        val info21Id = "khu123"
        val info21Password = "pass123!"
        val requestedUserId = 100L

        val member = createMember(id = requestedUserId, name = info21Id, status = UserStatus.ACTIVE)

        given(memberRepository.findByName(info21Id)).willReturn(member)

        // when & then
        assertThrows<ConflictException> {
            userRegistrationRequestService.createRegistrationRequest(
                info21Id = info21Id,
                info21Password = info21Password,
                requestedUserId = requestedUserId,
            )
        }

        verify(memberRepository).findByName(info21Id)
        verify(userRegistrationRequestRepository, never()).save(any())
    }

    @Test
    fun `createRegistrationRequest는 이미 대기중인 신청 요청이 있을 경우 ConflictException을 발생시킨다`() {
        // given
        val info21Id = "khu123"
        val info21Password = "pass123!"
        val requestedUserId = 100L

        val member = createMember(id = requestedUserId, name = info21Id, status = UserStatus.UNAUTHORIZED)

        given(memberRepository.findByName(info21Id)).willReturn(member)
        given(userRegistrationRequestRepository.existsByRequestedUserIdAndStatus(requestedUserId, UserRegistrationRequestStatus.PENDING))
            .willReturn(true)

        // when & then
        assertThrows<ConflictException> {
            userRegistrationRequestService.createRegistrationRequest(
                info21Id = info21Id,
                info21Password = info21Password,
                requestedUserId = requestedUserId,
            )
        }

        verify(memberRepository).findByName(info21Id)
        verify(userRegistrationRequestRepository).existsByRequestedUserIdAndStatus(requestedUserId, UserRegistrationRequestStatus.PENDING)
        verify(userRegistrationRequestRepository, never()).save(any())
    }

    @Test
    fun `createRegistrationRequest는 포털 로그인 인증 실패 시 UnauthorizedException을 발생시킨다`() {
        // given
        val info21Id = "khu123"
        val info21Password = "pass123!"
        val requestedUserId = 100L

        val member = createMember(id = requestedUserId, name = info21Id, status = UserStatus.UNAUTHORIZED)

        given(memberRepository.findByName(info21Id)).willReturn(member)
        given(userRegistrationRequestRepository.existsByRequestedUserIdAndStatus(requestedUserId, UserRegistrationRequestStatus.PENDING))
            .willReturn(false)
        given(khuisClient.authenticate(info21Id, info21Password)).willReturn(false)

        // when & then
        assertThrows<UnauthorizedException> {
            userRegistrationRequestService.createRegistrationRequest(
                info21Id = info21Id,
                info21Password = info21Password,
                requestedUserId = requestedUserId,
            )
        }

        verify(memberRepository).findByName(info21Id)
        verify(userRegistrationRequestRepository).existsByRequestedUserIdAndStatus(requestedUserId, UserRegistrationRequestStatus.PENDING)
        verify(khuisClient).authenticate(info21Id, info21Password)
        verify(userRegistrationRequestRepository, never()).save(any())
    }
}
