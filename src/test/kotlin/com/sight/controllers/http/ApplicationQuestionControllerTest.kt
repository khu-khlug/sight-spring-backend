package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.CreateApplicationQuestionRequest
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.application.ApplicationQuestion
import com.sight.service.ApplicationQuestionService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(
    ApplicationQuestionController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
class ApplicationQuestionControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var applicationQuestionService: ApplicationQuestionService

    @BeforeEach
    fun setUp() {
        val requester = Requester(userId = 1L, role = UserRole.MANAGER)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                requester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_MANAGER")),
            )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `가입 신청서 문항 생성 API는 201 Created와 생성된 문항을 반환한다`() {
        val request =
            CreateApplicationQuestionRequest(
                title = "자기소개",
                description = "자기소개를 작성해주세요.",
                minLength = 100,
            )
        val now = LocalDateTime.now()
        given(applicationQuestionService.createQuestion(request.title, request.description, request.minLength))
            .willReturn(
                ApplicationQuestion(
                    id = "question-ulid",
                    title = request.title,
                    description = request.description,
                    minLength = request.minLength,
                    order = null,
                    isExposed = false,
                    createdAt = now,
                    updatedAt = now,
                ),
            )

        mockMvc.perform(
            post("/manager/application-questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("question-ulid"))
            .andExpect(jsonPath("$.order").isEmpty)
            .andExpect(jsonPath("$.isExposed").value(false))

        verify(applicationQuestionService).createQuestion(request.title, request.description, request.minLength)
    }

    @Test
    fun `가입 신청서 문항 생성 API는 빈 제목을 받으면 400 Bad Request를 반환한다`() {
        val request =
            CreateApplicationQuestionRequest(
                title = " ",
                description = "자기소개를 작성해주세요.",
                minLength = 100,
            )

        mockMvc.perform(
            post("/manager/application-questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `가입 신청서 문항 목록 조회 API는 요청한 문항을 반환한다`() {
        given(applicationQuestionService.listQuestionsByIds(listOf("question-2", "question-1")))
            .willReturn(
                listOf(
                    ApplicationQuestion(
                        id = "question-2",
                        title = "둘째 문항",
                        description = "둘째 설명",
                        minLength = 20,
                        order = 2,
                        isExposed = true,
                    ),
                    ApplicationQuestion(
                        id = "question-1",
                        title = "첫 문항",
                        description = "첫 설명",
                        minLength = 10,
                        order = 1,
                        isExposed = true,
                    ),
                ),
            )

        mockMvc.perform(
            get("/application-questions")
                .queryParam("applicationQuestionIds", "question-2", "question-1"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.questions[0].id").value("question-2"))
            .andExpect(jsonPath("$.questions[1].id").value("question-1"))

        verify(applicationQuestionService).listQuestionsByIds(listOf("question-2", "question-1"))
    }
}
