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
import com.sight.service.GroupRecordAttachmentService
import com.sight.service.RecordFileVerificationResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.verifyNoInteractions
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@WebMvcTest(
    com.sight.controllers.http.GroupRecordFilesUploadedController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupRecordFilesUploadedControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: GroupRecordAttachmentService

    @MockBean
    private lateinit var groups: GroupRepository

    @MockBean
    private lateinit var members: GroupMemberRepository

    @MockBean
    private lateinit var authorizationRepository: GroupAuthorizationRepository

    @BeforeEach
    fun setUp() {
        given(groups.findById(1L)).willReturn(Optional.of(testGroup()))
        given(members.existsByGroupIdAndMemberId(1L, 2L)).willReturn(true)
        authenticate(2L, UserRole.USER)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `전체 성공과 부분 성공과 전체 실패를 각각 다른 HTTP 상태로 반환한다`() {
        val success = RecordFileVerificationResult("ok", true, null)
        val failure = RecordFileVerificationResult("pending", false, "NOT_UPLOADED")
        given(service.updateGroupRecordFilesUploaded(1L, 2L, listOf("ok"))).willReturn(listOf(success))
        given(service.updateGroupRecordFilesUploaded(1L, 2L, listOf("ok", "pending"))).willReturn(listOf(success, failure))
        given(service.updateGroupRecordFilesUploaded(1L, 2L, listOf("pending"))).willReturn(listOf(failure))

        mockMvc.perform(request("""{"fileIds":["ok"]}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.results[0].isSuccess").value(true))
        mockMvc.perform(request("""{"fileIds":["ok","pending"]}"""))
            .andExpect(status().isPartialContent)
            .andExpect(jsonPath("$.results[1].fileId").value("pending"))
            .andExpect(jsonPath("$.results[1].reason").value("NOT_UPLOADED"))
        mockMvc.perform(request("""{"fileIds":["pending"]}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.results[0].isSuccess").value(false))
    }

    @Test
    fun `빈 배열이나 숫자 파일 아이디는 서비스에 전달하지 않는다`() {
        listOf("""{"fileIds":[]}""", """{"fileIds":[42]}""").forEach { payload ->
            mockMvc.perform(request(payload)).andExpect(status().isBadRequest)
        }
        verifyNoInteractions(service)
    }

    @Test
    fun `그룹에 속하지 않은 운영진도 업로드 완료를 알릴 수 없다`() {
        authenticate(9L, UserRole.MANAGER)

        mockMvc.perform(request("""{"fileIds":["ok"]}""")).andExpect(status().isForbidden)

        verifyNoInteractions(service)
    }

    private fun request(payload: String) = post("/groups/1/records/files/uploaded").contentType(MediaType.APPLICATION_JSON).content(payload)

    private fun authenticate(
        memberId: Long,
        role: UserRole,
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                Requester(userId = memberId, role = role),
                null,
                listOf(SimpleGrantedAuthority("ROLE_${role.name}")),
            )
    }
}
