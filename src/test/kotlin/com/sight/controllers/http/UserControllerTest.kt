package com.sight.controllers.http

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.UserDiscordService
import com.sight.service.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.never
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    UserController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var userDiscordService: UserDiscordService

    @MockBean
    private lateinit var userService: UserService

    private val requester = Requester(userId = 123L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                requester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `개인 GitHub 사용 설정 조회는 현재 회원의 값을 200으로 반환한다`() {
        given(userService.getPreferenceByUserId(requester.userId)).willReturn(true)

        mockMvc.perform(get("/users/@me/preference"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.usePersonalGithubUsage").value(true))

        verify(userService).getPreferenceByUserId(requester.userId)
    }

    @Test
    fun `저장된 설정이 없으면 기본값 false를 200으로 반환한다`() {
        given(userService.getPreferenceByUserId(requester.userId)).willReturn(false)

        mockMvc.perform(get("/users/@me/preference"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.usePersonalGithubUsage").value(false))
    }

    @Test
    fun `개인 GitHub 사용 설정 변경 요청은 204를 반환한다`() {
        mockMvc.perform(
            patch("/users/@me/preference")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"usePersonalGithubUsage":true}"""),
        )
            .andExpect(status().isNoContent)

        verify(userService).updatePreference(requester.userId, usePersonalGithubUsage = true)
    }

    @Test
    fun `변경할 설정이 없거나 불리언이 아닌 요청은 400을 반환하고 서비스를 호출하지 않는다`() {
        val invalidBodies =
            listOf(
                "{}",
                """{"usePersonalGithubUsage":null}""",
                """{"usePersonalGithubUsage":"true"}""",
                """{"otherPreference":true}""",
            )

        invalidBodies.forEach { body ->
            mockMvc.perform(
                patch("/users/@me/preference")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
        }

        verify(userService, never()).updatePreference(requester.userId, usePersonalGithubUsage = true)
        verify(userService, never()).updatePreference(requester.userId, usePersonalGithubUsage = false)
    }
}
