package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.domain.application.UserRegistrationRequest
import com.sight.domain.application.UserRegistrationRequestStatus
import com.sight.repository.UserRegistrationRequestRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertEquals

class UserRegistrationRequestServiceTest {
    private val userRegistrationRequestRepository: UserRegistrationRequestRepository = mock()
    private val userRegistrationRequestService =
        UserRegistrationRequestService(
            userRegistrationRequestRepository = userRegistrationRequestRepository,
        )

    @Test
    fun `approve updates user registration request status to approved`() {
        val requestId = "registration-request-id"
        val userRegistrationRequest =
            UserRegistrationRequest(
                id = requestId,
                requestedUserId = 1L,
                status = UserRegistrationRequestStatus.PENDING,
            )
        whenever(userRegistrationRequestRepository.findById(requestId))
            .thenReturn(Optional.of(userRegistrationRequest))
        val captor = argumentCaptor<UserRegistrationRequest>()
        whenever(userRegistrationRequestRepository.save(any()))
            .thenAnswer { it.arguments[0] as UserRegistrationRequest }

        userRegistrationRequestService.approve(requestId)

        verify(userRegistrationRequestRepository).save(captor.capture())
        assertEquals(UserRegistrationRequestStatus.APPROVED, captor.firstValue.status)
    }

    @Test
    fun `approve throws not found when user registration request does not exist`() {
        val requestId = "missing-request-id"
        whenever(userRegistrationRequestRepository.findById(requestId)).thenReturn(Optional.empty())

        assertThrows<NotFoundException> {
            userRegistrationRequestService.approve(requestId)
        }

        verify(userRegistrationRequestRepository).findById(requestId)
    }
}
