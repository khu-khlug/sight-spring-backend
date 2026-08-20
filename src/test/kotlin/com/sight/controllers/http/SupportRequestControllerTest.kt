package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.CreateSupportRequestCommentRequest
import com.sight.controllers.http.dto.CreateSupportRequestRequest
import com.sight.controllers.http.dto.UpdateSupportRequestRequest
import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.supportrequest.SupportRequest
import com.sight.domain.supportrequest.SupportRequestCategory
import com.sight.domain.supportrequest.SupportRequestComment
import com.sight.service.SupportRequestCommentResult
import com.sight.service.SupportRequestDetail
import com.sight.service.SupportRequestListResult
import com.sight.service.SupportRequestService
import com.sight.service.SupportRequestSummary
import com.sight.service.SupportRequestUser
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
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(
    SupportRequestController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class)
@EnableAspectJAutoProxy
class SupportRequestControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var supportRequestService: SupportRequestService

    @BeforeEach
    fun setUp() {
        authenticate(UserRole.USER)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `지원 신청 생성 API는 201 Created와 지원 신청을 반환한다`() {
        val request = CreateSupportRequestRequest(SupportRequestCategory.SERVER_SPACE, "서버 공간", "프로젝트 서버가 필요합니다")
        val category = checkNotNull(request.category)
        val title = checkNotNull(request.title)
        val content = checkNotNull(request.content)
        given(supportRequestService.createSupportRequest(1L, category, title, content))
            .willReturn(summary())

        mockMvc.perform(
            post("/support-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("support-request-1"))
            .andExpect(jsonPath("$.requester.userId").value(1))
            .andExpect(jsonPath("$.requester.name").value("신청자"))
            .andExpect(jsonPath("$.hasComments").value(false))

        verify(supportRequestService).createSupportRequest(1L, category, title, content)
    }

    @Test
    fun `지원 신청 생성 API는 공백 제목을 거절한다`() {
        mockMvc.perform(
            post("/support-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"category":"OTHER","title":" ","content":"내용"}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `모든 인증 회원은 지원 신청 목록을 조회할 수 있다`() {
        given(supportRequestService.listSupportRequests(0, 20, null))
            .willReturn(SupportRequestListResult(1, listOf(summary())))

        mockMvc.perform(get("/support-requests"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.supportRequests[0].id").value("support-request-1"))
    }

    @Test
    fun `모든 인증 회원은 다른 회원의 지원 신청 상세와 댓글을 조회할 수 있다`() {
        val supportRequest = summary().supportRequest
        val comment = commentResult()
        given(supportRequestService.getSupportRequestById(supportRequest.id))
            .willReturn(SupportRequestDetail(supportRequest, SupportRequestUser(2L, "다른 신청자"), listOf(comment)))

        mockMvc.perform(get("/support-requests/${supportRequest.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requester.userId").value(2))
            .andExpect(jsonPath("$.comments[0].id").value("comment-1"))
    }

    @Test
    fun `지원 신청 목록 API는 허용되지 않은 카테고리를 거절한다`() {
        mockMvc.perform(get("/support-requests").queryParam("category", "INVALID"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `신청자는 자신의 지원 신청을 수정할 수 있다`() {
        val request = UpdateSupportRequestRequest(SupportRequestCategory.BOOK, "변경 제목", "변경 내용")
        val category = checkNotNull(request.category)
        val title = checkNotNull(request.title)
        val content = checkNotNull(request.content)
        given(
            supportRequestService.updateSupportRequest(
                "support-request-1",
                1L,
                category,
                title,
                content,
            ),
        ).willReturn(summary())

        mockMvc.perform(
            put("/support-requests/support-request-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("support-request-1"))

        verify(supportRequestService).updateSupportRequest(
            "support-request-1",
            1L,
            category,
            title,
            content,
        )
    }

    @Test
    fun `운영진만 지원 신청을 삭제할 수 있다`() {
        mockMvc.perform(delete("/support-requests/support-request-1"))
            .andExpect(status().isForbidden)

        authenticate(UserRole.MANAGER)
        mockMvc.perform(delete("/support-requests/support-request-1"))
            .andExpect(status().isNoContent)

        verify(supportRequestService).deleteSupportRequest("support-request-1")
    }

    @Test
    fun `신청자와 운영진은 댓글을 등록할 수 있고 일반 회원 역할을 서비스에 전달한다`() {
        val request = CreateSupportRequestCommentRequest("추가 내용")
        val content = checkNotNull(request.content)
        given(supportRequestService.createSupportRequestComment("support-request-1", 1L, false, content))
            .willReturn(commentResult())

        mockMvc.perform(
            post("/support-requests/support-request-1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.author.userId").value(1))

        verify(supportRequestService).createSupportRequestComment("support-request-1", 1L, false, content)
    }

    @Test
    fun `인증되지 않은 회원은 지원 신청을 조회할 수 없다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )

        mockMvc.perform(get("/support-requests"))
            .andExpect(status().isUnauthorized)
    }

    private fun summary(): SupportRequestSummary {
        val supportRequest =
            SupportRequest(
                id = "support-request-1",
                requesterId = 1L,
                category = SupportRequestCategory.SERVER_SPACE,
                title = "서버 공간",
                content = "프로젝트 서버가 필요합니다",
                createdAt = Instant.parse("2026-08-21T09:00:00Z"),
                updatedAt = Instant.parse("2026-08-21T09:00:00Z"),
            )
        return SupportRequestSummary(supportRequest, SupportRequestUser(1L, "신청자"), false)
    }

    private fun commentResult(): SupportRequestCommentResult {
        val supportRequest = summary().supportRequest
        val comment =
            SupportRequestComment(
                id = "comment-1",
                supportRequest = supportRequest,
                authorId = 1L,
                content = "추가 내용",
                createdAt = Instant.parse("2026-08-21T09:10:00Z"),
            )
        return SupportRequestCommentResult(comment, SupportRequestUser(1L, "신청자"))
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
