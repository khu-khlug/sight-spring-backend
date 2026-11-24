package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.UnprocessableEntityException
import com.sight.core.groupauth.GroupAuthAspect
import com.sight.core.groupauth.GroupAuthorizationService
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRecordAuthor
import com.sight.repository.GroupRepository
import com.sight.service.GroupRecordAttachmentService
import com.sight.service.GroupRecordReadService
import com.sight.service.GroupRecordWriteService
import com.sight.service.RecordFileVerificationResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.willThrow
import org.mockito.kotlin.given
import org.mockito.kotlin.verifyNoInteractions
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@WebMvcTest(
    com.sight.controllers.http.GroupRecordController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupRecordControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var reads: GroupRecordReadService

    @MockBean
    private lateinit var writes: GroupRecordWriteService

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
        givenGroup(GroupVisibility.MEMBER_ONLY)
        given(members.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)
        authenticate(MEMBER_ID, UserRole.USER)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `업로드 완료 확인은 전체 성공 시 200, 일부 성공 시 206, 전체 실패 시 400을 반환한다`() {
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
    fun `파일 아이디 배열이 비어 있거나 숫자 아이디가 포함되면 400을 반환한다`() {
        listOf("""{"fileIds":[]}""", """{"fileIds":[42]}""").forEach { payload ->
            mockMvc.perform(request(payload)).andExpect(status().isBadRequest)
        }
        verifyNoInteractions(service)
    }

    @Test
    fun `그룹원이 아니면 그룹원 전용 API를 호출할 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.USER)

        expectStatus(403, memberEndpoints())

        verifyNoServiceInteractions()
    }

    @Test
    fun `운영진이라도 그룹원이 아니면 그룹원 전용 API를 호출할 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.MANAGER)

        expectStatus(403, memberEndpoints())

        verifyNoServiceInteractions()
    }

    @Test
    fun `그룹원은 그룹원 전용 API를 호출할 수 있다`() {
        memberEndpoints().forEach { it.reachService(MEMBER_ID) }

        expectStatus(REACHED_SERVICE, memberEndpoints())
    }

    @Test
    fun `그룹장은 그룹원 목록에 없어도 그룹원 전용 API를 호출할 수 있다`() {
        authenticate(LEADER_ID, UserRole.USER)
        memberEndpoints().forEach { it.reachService(LEADER_ID) }

        expectStatus(REACHED_SERVICE, memberEndpoints())
    }

    @Test
    fun `비공개 그룹에서는 그룹원이 아니면 열람 API를 호출할 수 없다`() {
        givenGroup(GroupVisibility.PRIVATE)
        authenticate(OUTSIDER_ID, UserRole.USER)

        expectStatus(403, viewerEndpoints())

        verifyNoServiceInteractions()
    }

    @Test
    fun `완전 공개 그룹에서는 그룹원이 아니어도 열람 API를 호출할 수 있다`() {
        givenGroup(GroupVisibility.PUBLIC)
        authenticate(OUTSIDER_ID, UserRole.USER)
        viewerEndpoints().forEach { it.reachService(OUTSIDER_ID) }

        expectStatus(REACHED_SERVICE, viewerEndpoints())
    }

    @Test
    fun `기록 작성자인 그룹원은 기록 수정 API를 호출할 수 있다`() {
        given(authorizationRepository.findRecordAuthorById(RECORD_ID)).willReturn(GroupRecordAuthor(GROUP_ID, MEMBER_ID))
        recordAuthorEndpoint().reachService(MEMBER_ID)

        expectStatus(REACHED_SERVICE, listOf(recordAuthorEndpoint()))
    }

    @Test
    fun `기록 작성자가 아닌 그룹원은 기록 수정 API를 호출할 수 없다`() {
        given(authorizationRepository.findRecordAuthorById(RECORD_ID)).willReturn(GroupRecordAuthor(GROUP_ID, LEADER_ID))

        expectStatus(403, listOf(recordAuthorEndpoint()))

        verifyNoServiceInteractions()
    }

    @Test
    fun `기록 작성자였더라도 그룹원이 아니면 기록 수정 API를 호출할 수 없다`() {
        given(authorizationRepository.findRecordAuthorById(RECORD_ID)).willReturn(GroupRecordAuthor(GROUP_ID, OUTSIDER_ID))
        authenticate(OUTSIDER_ID, UserRole.USER)

        expectStatus(403, listOf(recordAuthorEndpoint()))

        verifyNoServiceInteractions()
    }

    @Test
    fun `경로의 그룹이 아닌 다른 그룹에 속한 기록은 기록 수정 API로 수정할 수 없다`() {
        given(authorizationRepository.findRecordAuthorById(RECORD_ID)).willReturn(GroupRecordAuthor(OTHER_GROUP_ID, MEMBER_ID))

        expectStatus(403, listOf(recordAuthorEndpoint()))

        verifyNoServiceInteractions()
    }

    @Test
    fun `기록이 없으면 기록 수정 API는 404를 반환한다`() {
        given(authorizationRepository.findRecordAuthorById(RECORD_ID)).willReturn(null)

        expectStatus(404, listOf(recordAuthorEndpoint()))

        verifyNoServiceInteractions()
    }

    @Test
    fun `그룹이 없으면 그룹 권한이 필요한 모든 기록 API는 404를 반환한다`() {
        given(groups.findById(GROUP_ID)).willReturn(Optional.empty())

        expectStatus(404, allEndpoints())

        verifyNoServiceInteractions()
    }

    @Test
    fun `인증되지 않으면 그룹 권한이 필요한 모든 기록 API는 401을 반환한다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )

        expectStatus(401, allEndpoints())

        verifyNoServiceInteractions()
    }

    private fun memberEndpoints(): List<Endpoint> =
        listOf(
            Endpoint(
                "POST /groups/$GROUP_ID/cards/$CARD_ID/records",
                { post("/groups/$GROUP_ID/cards/$CARD_ID/records").json("""{"content":"본문","files":[]}""") },
            ) { requester -> willThrow(reached()).given(writes).createGroupRecord(GROUP_ID, CARD_ID, requester, "본문", emptyList()) },
            Endpoint("POST /groups/$GROUP_ID/records/files/uploaded", { request("""{"fileIds":["ok"]}""") }) { requester ->
                willThrow(reached()).given(service).updateGroupRecordFilesUploaded(GROUP_ID, requester, listOf("ok"))
            },
        )

    private fun viewerEndpoints(): List<Endpoint> =
        listOf(
            Endpoint("GET /groups/$GROUP_ID/cards/$CARD_ID/records", { get("/groups/$GROUP_ID/cards/$CARD_ID/records") }) { requester ->
                willThrow(reached()).given(reads).listGroupRecords(GROUP_ID, CARD_ID, requester, 0L, 10, false)
            },
            Endpoint("GET /groups/$GROUP_ID/records/$RECORD_ID", { get("/groups/$GROUP_ID/records/$RECORD_ID") }) { requester ->
                willThrow(reached()).given(reads).getGroupRecordById(GROUP_ID, RECORD_ID, requester)
            },
        )

    private fun recordAuthorEndpoint(): Endpoint =
        Endpoint(
            "PATCH /groups/$GROUP_ID/records/$RECORD_ID",
            { patch("/groups/$GROUP_ID/records/$RECORD_ID").json("""{"content":"본문","files":[]}""") },
        ) { requester -> willThrow(reached()).given(writes).updateGroupRecord(GROUP_ID, RECORD_ID, requester, "본문", emptyList()) }

    private fun allEndpoints(): List<Endpoint> = memberEndpoints() + viewerEndpoints() + recordAuthorEndpoint()

    private fun request(payload: String) = post("/groups/1/records/files/uploaded").contentType(MediaType.APPLICATION_JSON).content(payload)

    private fun MockHttpServletRequestBuilder.json(payload: String): MockHttpServletRequestBuilder =
        contentType(MediaType.APPLICATION_JSON).content(payload)

    private fun expectStatus(
        expected: Int,
        endpoints: List<Endpoint>,
    ) {
        endpoints.forEach { endpoint ->
            val actual = mockMvc.perform(endpoint.request()).andReturn().response.status
            assertEquals(expected, actual, endpoint.name)
        }
    }

    private fun verifyNoServiceInteractions() {
        verifyNoInteractions(service, reads, writes)
    }

    private fun reached() = UnprocessableEntityException("서비스가 호출되었다")

    private fun givenGroup(visibility: GroupVisibility) {
        given(
            groups.findById(GROUP_ID),
        ).willReturn(Optional.of(testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID, visibility = visibility)))
    }

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

    private class Endpoint(
        val name: String,
        val request: () -> MockHttpServletRequestBuilder,
        val reachService: (requesterId: Long) -> Unit,
    )

    companion object {
        private const val GROUP_ID = 1L
        private const val OTHER_GROUP_ID = 7L
        private const val CARD_ID = 3L
        private const val RECORD_ID = 4L
        private const val LEADER_ID = 1L
        private const val MEMBER_ID = 2L
        private const val OUTSIDER_ID = 9L
        private const val REACHED_SERVICE = 422
    }
}
