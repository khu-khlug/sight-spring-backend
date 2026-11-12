package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.group.GroupRecordType
import com.sight.repository.dto.SavedRecordView
import com.sight.service.GroupRecordArchiveService
import com.sight.service.SavedRecordListResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(
    com.sight.controllers.http.SavedRecordController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class)
@EnableAspectJAutoProxy
class SavedRecordControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var groupRecordArchiveService: GroupRecordArchiveService

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `인증된 사용자는 본인이 보관한 기록 목록을 문자열 아이디와 UTC 시각으로 받는다`() {
        authenticate(MEMBER_ID, UserRole.USER)
        given(groupRecordArchiveService.listSavedRecords(MEMBER_ID, 0)).willReturn(
            SavedRecordListResult(
                page = 0,
                totalCount = 1,
                receivedCount = 1,
                records =
                    listOf(
                        SavedRecordView(
                            recordId = 30L,
                            type = GroupRecordType.TIPTAP,
                            content = "{\"type\":\"doc\"}",
                            groupId = 10L,
                            groupName = "그룹",
                            cardId = 20L,
                            cardTitle = "카드",
                            isDeleted = false,
                            isCardDeleted = true,
                            authorName = "작성자",
                            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                            savedAt = Instant.parse("2026-01-02T00:00:00Z"),
                        ),
                    ),
            ),
        )

        mockMvc.perform(get("/saved-records"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.totalCount").value(1))
            .andExpect(jsonPath("$.receivedCount").value(1))
            .andExpect(jsonPath("$.records[0].recordId").value("30"))
            .andExpect(jsonPath("$.records[0].groupId").value("10"))
            .andExpect(jsonPath("$.records[0].cardId").value("20"))
            .andExpect(jsonPath("$.records[0].type").value("TIPTAP"))
            .andExpect(jsonPath("$.records[0].isDeleted").value(false))
            .andExpect(jsonPath("$.records[0].isCardDeleted").value(true))
            .andExpect(jsonPath("$.records[0].createdAt").value("2026-01-01T00:00:00Z"))
            .andExpect(jsonPath("$.records[0].savedAt").value("2026-01-02T00:00:00Z"))
            .andExpect(jsonPath("$.records[0].files").doesNotExist())
    }

    @Test
    fun `음수·소수·숫자가 아닌 page는 400을 반환한다`() {
        authenticate(MEMBER_ID, UserRole.USER)

        listOf("-1", "1.5", "abc").forEach { page ->
            mockMvc.perform(get("/saved-records").param("page", page))
                .andExpect(status().isBadRequest)
        }
        verify(groupRecordArchiveService, never()).listSavedRecords(any(), any())
    }

    @Test
    fun `인증되지 않은 요청은 보관한 기록 목록을 조회할 수 없다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )

        mockMvc.perform(get("/saved-records"))
            .andExpect(status().isUnauthorized)
    }

    private fun authenticate(
        userId: Long,
        role: UserRole,
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                Requester(userId = userId, role = role),
                null,
                listOf(SimpleGrantedAuthority("ROLE_${role.name}")),
            )
    }

    companion object {
        private const val MEMBER_ID = 2L
    }
}
