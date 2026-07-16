package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.CreateUserRegistrationRequest
import com.sight.controllers.http.dto.UpdateUserRegistrationRequestStatusRequest
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.application.UserRegistrationRequest
import com.sight.domain.application.UserRegistrationRequestStatus
import com.sight.service.UserRegistrationRequestService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

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
    fun `createRegistrationRequest는 모든 검증을 통과하면 201 Created와 신청 상세 정보를 반환한다`() {
        // given
        val info21Id = "khu123"
        val info21Password = "pass123!"
        val requestDto = CreateUserRegistrationRequest(info21Id = info21Id, info21Password = info21Password)

        val now = LocalDateTime.now()
        val expectedRequest =
            UserRegistrationRequest(
                id = "req-ulid",
                requestedUserId = testRequester.userId,
                status = UserRegistrationRequestStatus.PENDING,
                createdAt = now,
                updatedAt = now,
            )

        given(
            userRegistrationRequestService.createRegistrationRequest(
                info21Id = info21Id,
                info21Password = info21Password,
                requestedUserId = testRequester.userId,
            ),
        ).willReturn(expectedRequest)

        // when & then
        mockMvc.perform(
            post("/user-registration-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("req-ulid"))
            .andExpect(jsonPath("$.requestedUserId").value(testRequester.userId))
            .andExpect(jsonPath("$.status").value(UserRegistrationRequestStatus.PENDING.name))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())

        verify(userRegistrationRequestService).createRegistrationRequest(
            info21Id = info21Id,
            info21Password = info21Password,
            requestedUserId = testRequester.userId,
        )
    }

    @Test
    fun `createRegistrationRequest는 info21Id가 비어있으면 400 Bad Request를 반환한다`() {
        // given
        val requestDto = CreateUserRegistrationRequest(info21Id = " ", info21Password = "pass123!")

        // when & then
        mockMvc.perform(
            post("/user-registration-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `createRegistrationRequest는 info21Password가 비어있으면 400 Bad Request를 반환한다`() {
        // given
        val requestDto = CreateUserRegistrationRequest(info21Id = "khu123", info21Password = "")

        // when & then
        mockMvc.perform(
            post("/user-registration-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `updateStatus approves user registration request and returns content`() {
        val requestId = "registration-request-id"
        given(userRegistrationRequestService.approve(requestId))
            .willReturn(
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
        mockMvc.perform(
            put("/manager/user-registration-requests/registration-request-id")
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
