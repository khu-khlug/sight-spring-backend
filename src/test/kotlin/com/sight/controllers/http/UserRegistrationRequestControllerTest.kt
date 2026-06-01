package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.UpdateUserRegistrationRequestStatusRequest
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.application.UserRegistrationRequest
import com.sight.domain.application.UserRegistrationRequestStatus
import com.sight.service.UserRegistrationRequestService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    UserRegistrationRequestController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
class UserRegistrationRequestControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var userRegistrationRequestService: UserRegistrationRequestService

    private val testRequester = Requester(userId = 12345L, role = UserRole.MANAGER)

    @BeforeEach
    fun setUp() {
        val auth =
            UsernamePasswordAuthenticationToken(
                testRequester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_MANAGER")),
            )
        SecurityContextHolder.getContext().authentication = auth
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `updateStatus approves user registration request and returns content`() {
        val requestId = "registration-request-id"
        whenever(userRegistrationRequestService.approve(requestId))
            .thenReturn(
                UserRegistrationRequest(
                    id = requestId,
                    requestedUserId = 1L,
                    status = UserRegistrationRequestStatus.APPROVED,
                ),
            )

        mockMvc.perform(
            put("/manager/user-registration-requests/$requestId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        UpdateUserRegistrationRequestStatusRequest(status = "approved"),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("승인되었습니다"))

        verify(userRegistrationRequestService).approve(requestId)
    }

    @Test
    fun `updateStatus returns bad request when status is unsupported`() {
        val requestId = "registration-request-id"

        mockMvc.perform(
            put("/manager/user-registration-requests/$requestId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        UpdateUserRegistrationRequestStatusRequest(status = "pending"),
                    ),
                ),
        )
            .andExpect(status().isBadRequest)
    }
}
