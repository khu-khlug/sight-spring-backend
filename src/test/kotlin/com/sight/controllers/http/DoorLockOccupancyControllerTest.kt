package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.UpdateDoorLockOccupantsRequest
import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.DoorLockOccupancyResult
import com.sight.service.DoorLockOccupancyService
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(
    DoorLockOccupancyController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class)
@EnableAspectJAutoProxy
class DoorLockOccupancyControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var doorLockOccupancyService: DoorLockOccupancyService

    @BeforeEach
    fun setUp() {
        authenticate(UserRole.SYSTEM)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `시스템은 재실자 snapshot을 교체한다`() {
        val request =
            UpdateDoorLockOccupantsRequest(
                occupants = listOf("2025101234", "2025109999"),
            )

        mockMvc.perform(
            put("/occupants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OK"))

        verify(doorLockOccupancyService).updateOccupants(
            eq(listOf(2025101234L, 2025109999L)),
            any(),
        )
    }

    @Test
    fun `빈 재실자 snapshot을 허용한다`() {
        val request = UpdateDoorLockOccupantsRequest(occupants = emptyList())

        mockMvc.perform(
            put("/occupants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)

        verify(doorLockOccupancyService).updateOccupants(eq(emptyList()), any())
    }

    @Test
    fun `재실자 목록이 누락되면 400 Bad Request를 반환한다`() {
        mockMvc.perform(
            put("/occupants")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)

        verify(doorLockOccupancyService, never()).updateOccupants(any(), any())
    }

    @Test
    fun `학번 형식이 잘못되면 400 Bad Request를 반환한다`() {
        val request = UpdateDoorLockOccupantsRequest(occupants = listOf("2025-1234"))

        mockMvc.perform(
            put("/occupants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)

        verify(doorLockOccupancyService, never()).updateOccupants(any(), any())
    }

    @Test
    fun `재실자 목록에 null이 포함되면 400 Bad Request를 반환한다`() {
        val request = """{"occupants":[null]}"""

        mockMvc.perform(
            put("/occupants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request),
        )
            .andExpect(status().isBadRequest)

        verify(doorLockOccupancyService, never()).updateOccupants(any(), any())
    }

    @Test
    fun `재실자가 24명을 초과하면 400 Bad Request를 반환한다`() {
        val request =
            UpdateDoorLockOccupantsRequest(
                occupants = (1..25).map { number -> "20251${number.toString().padStart(5, '0')}" },
            )

        mockMvc.perform(
            put("/occupants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)

        verify(doorLockOccupancyService, never()).updateOccupants(any(), any())
    }

    @Test
    fun `일반 회원은 재실자 snapshot을 교체할 수 없다`() {
        authenticate(UserRole.USER)
        val request = UpdateDoorLockOccupantsRequest(occupants = emptyList())

        mockMvc.perform(
            put("/occupants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `일반 회원은 재실자 이름과 동기화 시각을 조회한다`() {
        authenticate(UserRole.USER)
        val syncedAt = Instant.parse("2026-09-12T10:30:00Z")
        given(doorLockOccupancyService.listOccupants())
            .willReturn(
                DoorLockOccupancyResult(
                    occupantNames = listOf("김철수", "홍길동"),
                    lastSyncedAt = syncedAt,
                ),
            )

        mockMvc.perform(get("/occupants"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.occupants[0].name").value("김철수"))
            .andExpect(jsonPath("$.occupants[1].name").value("홍길동"))
            .andExpect(jsonPath("$.occupants[0].userId").doesNotExist())
            .andExpect(jsonPath("$.occupants[0].studentId").doesNotExist())
            .andExpect(jsonPath("$.lastSyncedAt").value("2026-09-12T10:30:00Z"))
    }

    @Test
    fun `운영진도 재실 상태를 조회할 수 있다`() {
        authenticate(UserRole.MANAGER)
        given(doorLockOccupancyService.listOccupants())
            .willReturn(DoorLockOccupancyResult(emptyList(), null))

        mockMvc.perform(get("/occupants"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.occupants").isEmpty)
            .andExpect(jsonPath("$.lastSyncedAt").value(nullValue()))
    }

    @Test
    fun `시스템은 재실 상태를 조회할 수 없다`() {
        mockMvc.perform(get("/occupants"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `인증되지 않은 요청은 재실 상태를 조회할 수 없다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )

        mockMvc.perform(get("/occupants"))
            .andExpect(status().isUnauthorized)
    }

    private fun authenticate(role: UserRole) {
        val requester = Requester(userId = 1L, role = role)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                requester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_${role.name}")),
            )
    }
}
