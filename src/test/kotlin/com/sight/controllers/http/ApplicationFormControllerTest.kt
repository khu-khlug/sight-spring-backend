package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.CreateApplicationCommentRequest
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.application.ApplicationComment
import com.sight.domain.application.ApplicationForm
import com.sight.domain.application.ApplicationFormStatus
import com.sight.service.ApplicationFormService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(
    ApplicationFormController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
class ApplicationFormControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var applicationFormService: ApplicationFormService

    private val testRequester = Requester(userId = 12345L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val auth =
            UsernamePasswordAuthenticationToken(
                testRequester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
        SecurityContextHolder.getContext().authentication = auth
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `가입 신청서 댓글 생성 API가 정상 작동하면 201 Created와 생성된 댓글 상세 정보를 반환한다`() {
        // given
        val applicationFormId = "form-ulid"
        val content = "멋진 지원서입니다!"
        val requestDto = CreateApplicationCommentRequest(content = content)

        val now = LocalDateTime.now()
        val expectedComment =
            ApplicationComment(
                id = "comment-ulid",
                applicationFormId = applicationFormId,
                authorUserId = testRequester.userId,
                content = content,
                createdAt = now,
                updatedAt = now,
            )

        given(
            applicationFormService.createComment(
                applicationFormId = applicationFormId,
                authorUserId = testRequester.userId,
                content = content,
            ),
        ).willReturn(expectedComment)

        // when & then
        mockMvc.perform(
            post("/application-forms/$applicationFormId/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("comment-ulid"))
            .andExpect(jsonPath("$.applicationFormId").value(applicationFormId))
            .andExpect(jsonPath("$.authorUserId").value(testRequester.userId))
            .andExpect(jsonPath("$.content").value(content))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())

        verify(applicationFormService).createComment(
            applicationFormId = applicationFormId,
            authorUserId = testRequester.userId,
            content = content,
        )
    }

    @Test
    fun `가입 신청서 댓글 생성 API 호출 시 본문 내용이 비어있으면 400 Bad Request를 반환한다`() {
        // given
        val applicationFormId = "form-ulid"
        val requestDto = CreateApplicationCommentRequest(content = " ")

        // when & then
        mockMvc.perform(
            post("/application-forms/$applicationFormId/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `운영진 가입 신청서 목록 조회 API는 applications와 count를 반환한다`() {
        // given
        val managerRequester = Requester(userId = 12345L, role = UserRole.MANAGER)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                managerRequester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_MANAGER")),
            )
        val form =
            ApplicationForm(
                id = "form-ulid",
                info21Id = "info21-id",
                submittee = "홍길동",
                status = ApplicationFormStatus.SUBMITTED,
            )
        given(applicationFormService.listForms(1, emptyList(), null))
            .willReturn(PageImpl(listOf(form), PageRequest.of(0, 20), 1))

        // when & then
        mockMvc.perform(get("/manager/application-forms"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.applications[0].id").value("form-ulid"))

        verify(applicationFormService).listForms(1, emptyList(), null)
    }

    @Test
    fun `가입 신청서 합격 처리 API가 정상 작동하면 204 No Content를 반환한다`() {
        // given
        val applicationFormId = "form-ulid"
        val managerRequester = Requester(userId = 12345L, role = UserRole.MANAGER)
        val auth =
            UsernamePasswordAuthenticationToken(
                managerRequester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_MANAGER")),
            )
        SecurityContextHolder.getContext().authentication = auth

        val expectedForm =
            ApplicationForm(
                id = applicationFormId,
                info21Id = "info21-id",
                submittee = "홍길동",
                status = ApplicationFormStatus.PASSED,
            )

        given(
            applicationFormService.passApplicationForm(
                applicationFormId = applicationFormId,
                authorUserId = managerRequester.userId,
            ),
        ).willReturn(expectedForm)

        // when & then
        mockMvc.perform(
            patch("/application-forms/$applicationFormId/pass"),
        )
            .andExpect(status().isNoContent)

        verify(applicationFormService).passApplicationForm(
            applicationFormId = applicationFormId,
            authorUserId = managerRequester.userId,
        )
    }

    @Test
    fun `가입 신청서 불합격 처리 API가 정상 작동하면 204 No Content를 반환한다`() {
        // given
        val applicationFormId = "form-ulid"
        val managerRequester = Requester(userId = 12345L, role = UserRole.MANAGER)
        val auth =
            UsernamePasswordAuthenticationToken(
                managerRequester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_MANAGER")),
            )
        SecurityContextHolder.getContext().authentication = auth

        val expectedForm =
            ApplicationForm(
                id = applicationFormId,
                info21Id = "info21-id",
                submittee = "홍길동",
                status = ApplicationFormStatus.REJECTED,
            )

        given(
            applicationFormService.rejectApplicationForm(
                applicationFormId = applicationFormId,
                authorUserId = managerRequester.userId,
            ),
        ).willReturn(expectedForm)

        // when & then
        mockMvc.perform(
            patch("/application-forms/$applicationFormId/reject"),
        )
            .andExpect(status().isNoContent)

        verify(applicationFormService).rejectApplicationForm(
            applicationFormId = applicationFormId,
            authorUserId = managerRequester.userId,
        )
    }

    @Test
    fun `가입 신청서 중단 처리 API가 정상 작동하면 204 No Content를 반환한다`() {
        // given
        val applicationFormId = "form-ulid"
        val managerRequester = Requester(userId = 12345L, role = UserRole.MANAGER)
        val auth =
            UsernamePasswordAuthenticationToken(
                managerRequester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_MANAGER")),
            )
        SecurityContextHolder.getContext().authentication = auth

        val expectedForm =
            ApplicationForm(
                id = applicationFormId,
                info21Id = "info21-id",
                submittee = "홍길동",
                status = ApplicationFormStatus.SUSPENDED,
            )

        given(
            applicationFormService.suspendApplicationForm(
                applicationFormId = applicationFormId,
                authorUserId = managerRequester.userId,
            ),
        ).willReturn(expectedForm)

        // when & then
        mockMvc.perform(
            patch("/application-forms/$applicationFormId/suspend"),
        )
            .andExpect(status().isNoContent)

        verify(applicationFormService).suspendApplicationForm(
            applicationFormId = applicationFormId,
            authorUserId = managerRequester.userId,
        )
    }
}
