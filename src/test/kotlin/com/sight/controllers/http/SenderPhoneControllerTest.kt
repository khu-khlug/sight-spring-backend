package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.UpdateSenderPhoneRequest
import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.SenderPhoneService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    SenderPhoneController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class)
@EnableAspectJAutoProxy
class SenderPhoneControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var senderPhoneService: SenderPhoneService

    @BeforeEach
    fun setUp() {
        authenticate(UserRole.MANAGER)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `운영진은 공식 발신번호를 조회한다`() {
        given(senderPhoneService.getSenderPhone()).willReturn("029302266")

        mockMvc.perform(get("/manager/sender-phone"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.phone").value("029302266"))
    }

    @Test
    fun `운영진은 공식 발신번호를 변경한다`() {
        val request = UpdateSenderPhoneRequest(phone = "02-930-2266")

        mockMvc.perform(
            put("/manager/sender-phone")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNoContent)

        verify(senderPhoneService).updateSenderPhone("02-930-2266")
    }

    @Test
    fun `공식 발신번호가 누락되면 400 Bad Request를 반환한다`() {
        mockMvc.perform(
            put("/manager/sender-phone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `일반 회원은 공식 발신번호를 조회할 수 없다`() {
        authenticate(UserRole.USER)

        mockMvc.perform(get("/manager/sender-phone"))
            .andExpect(status().isForbidden)
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
