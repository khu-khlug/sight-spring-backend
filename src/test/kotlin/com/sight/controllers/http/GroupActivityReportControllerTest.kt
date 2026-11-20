package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuthAspect
import com.sight.core.groupauth.GroupAuthorizationService
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.ActivityReportListResult
import com.sight.service.ActivityReportResult
import com.sight.service.GroupActivityReportService
import com.sight.service.UploadLinkResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
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
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(
    GroupActivityReportController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupActivityReportControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var groupActivityReportService: GroupActivityReportService

    @MockBean
    private lateinit var groupRepository: GroupRepository

    @MockBean
    private lateinit var groupMemberRepository: GroupMemberRepository

    @MockBean
    private lateinit var groupAuthorizationRepository: GroupAuthorizationRepository

    private val report =
        ActivityReportResult(
            id = REPORT_ID,
            groupId = GROUP_ID,
            seminarId = "seminar-1",
            isPresentation = true,
            reportFileKey = "reports/file-1.pdf",
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        )

    @BeforeEach
    fun setUp() {
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID)))
        given(groupMemberRepository.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `활동보고 업로드 링크는 그룹장만 발급받을 수 있다`() {
        given(groupActivityReportService.getUploadLink(GROUP_ID, LEADER_ID, requestPath = UPLOAD_LINK_PATH))
            .willReturn(UploadLinkResult(url = "https://upload.example.com", fileKey = "reports/file-1.pdf", fileUploadId = "file-1"))

        assertOnlyLeaderAllowed(status().isOk) { post(UPLOAD_LINK_PATH) }
    }

    @Test
    fun `활동보고는 그룹장만 제출할 수 있다`() {
        given(
            groupActivityReportService.submitReport(
                groupId = GROUP_ID,
                requesterId = LEADER_ID,
                isPresentation = true,
                fileUploadId = "file-1",
                uploadLinkRequestPath = UPLOAD_LINK_PATH,
            ),
        ).willReturn(report)

        assertOnlyLeaderAllowed(status().isCreated) {
            post("/groups/$GROUP_ID/activity-report")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"isPresentation":true,"fileUploadId":"file-1"}""")
        }
    }

    @Test
    fun `활동보고는 그룹장만 수정할 수 있다`() {
        given(
            groupActivityReportService.editReport(
                groupId = GROUP_ID,
                reportId = REPORT_ID,
                requesterId = LEADER_ID,
                isPresentation = false,
                fileUploadId = null,
                uploadLinkRequestPath = UPLOAD_LINK_PATH,
            ),
        ).willReturn(report)

        assertOnlyLeaderAllowed(status().isOk) {
            patch("/groups/$GROUP_ID/activity-report/$REPORT_ID")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"isPresentation":false}""")
        }
    }

    @Test
    fun `활동보고는 그룹장과 운영진이 취소할 수 있다`() {
        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(delete("/groups/$GROUP_ID/activity-report/$REPORT_ID")).andExpect(status().isForbidden)

        authenticate(LEADER_ID, UserRole.USER)
        mockMvc.perform(delete("/groups/$GROUP_ID/activity-report/$REPORT_ID")).andExpect(status().isNoContent)

        authenticate(MANAGER_ID, UserRole.MANAGER)
        mockMvc.perform(delete("/groups/$GROUP_ID/activity-report/$REPORT_ID")).andExpect(status().isNoContent)
    }

    @Test
    fun `활동보고 목록은 그룹원과 운영진이 조회할 수 있다`() {
        given(groupActivityReportService.listReports(GROUP_ID)).willReturn(ActivityReportListResult(reports = emptyList()))

        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(get("/groups/$GROUP_ID/activity-report")).andExpect(status().isForbidden)

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(get("/groups/$GROUP_ID/activity-report")).andExpect(status().isOk)

        authenticate(MANAGER_ID, UserRole.MANAGER)
        mockMvc.perform(get("/groups/$GROUP_ID/activity-report")).andExpect(status().isOk)
    }

    private fun assertOnlyLeaderAllowed(
        allowedStatus: ResultMatcher,
        request: () -> RequestBuilder,
    ) {
        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(request()).andExpect(status().isForbidden)

        authenticate(MANAGER_ID, UserRole.MANAGER)
        mockMvc.perform(request()).andExpect(status().isForbidden)

        authenticate(LEADER_ID, UserRole.USER)
        mockMvc.perform(request()).andExpect(allowedStatus)
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
        private const val GROUP_ID = 1L
        private const val LEADER_ID = 1L
        private const val MEMBER_ID = 2L
        private const val OUTSIDER_ID = 3L
        private const val MANAGER_ID = 9L
        private const val REPORT_ID = "report-1"
        private const val UPLOAD_LINK_PATH = "/groups/$GROUP_ID/activity-report/upload-link"
    }
}
