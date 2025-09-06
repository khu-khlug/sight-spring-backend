package com.sight.service

import com.sight.domain.member.Member
import StudentStatus
import UserStatus
import com.sight.domain.auth.UserRole
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import kotlin.test.assertEquals

class AuthServiceTest {
    private val restTemplate = mock<org.springframework.web.client.RestTemplate>()
    private val memberRepository = mock<MemberRepository>()
    private val authService = AuthService(restTemplate, memberRepository, "endpoint", "api-key")

    @Test
    fun `getUserRole은 manager가 true인 멤버에 대해 MANAGER 역할을 반환한다`() {
        // Given
        val userId = 1L
        val managerMember =
            Member(
                id = userId,
                name = "manager",
                manager = true,
                realname = "Manager User",
                admission = "20",
                college = "Engineering",
                studentStatus = StudentStatus.UNDERGRADUATE,
                status = UserStatus.ACTIVE,
            )
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(managerMember))

        // When
        val role = authService.getUserRole(userId)

        // Then
        assertEquals(UserRole.MANAGER, role)
    }

    @Test
    fun `getUserRole은 manager가 false인 멤버에 대해 USER 역할을 반환한다`() {
        // Given
        val userId = 2L
        val regularMember =
            Member(
                id = userId,
                name = "user",
                manager = false,
                realname = "Regular User",
                admission = "21",
                college = "Science",
                studentStatus = StudentStatus.UNDERGRADUATE,
                status = UserStatus.ACTIVE,
            )
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(regularMember))

        // When
        val role = authService.getUserRole(userId)

        // Then
        assertEquals(UserRole.USER, role)
    }

    @Test
    fun `getUserRole은 존재하지 않는 사용자에 대해 예외를 발생시킨다`() {
        // Given
        val userId = 999L
        whenever(memberRepository.findById(userId)).thenReturn(Optional.empty())

        // When & Then
        assertThrows<ResponseStatusException> {
            authService.getUserRole(userId)
        }
    }

    @Test
    fun `createRequester는 사용자 ID와 역할을 가진 Requester를 생성한다`() {
        // Given
        val userId = 1L
        val managerMember =
            Member(
                id = userId,
                name = "manager",
                manager = true,
                realname = "Manager User",
                admission = "20",
                college = "Engineering",
                studentStatus = StudentStatus.UNDERGRADUATE,
                status = UserStatus.ACTIVE,
            )
        whenever(memberRepository.findById(userId)).thenReturn(Optional.of(managerMember))

        // When
        val requester = authService.createRequester(userId)

        // Then
        assertEquals(userId, requester.userId)
        assertEquals(UserRole.MANAGER, requester.role)
    }
}
