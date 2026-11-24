package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.UnprocessableEntityException
import com.sight.core.groupauth.GroupAuthAspect
import com.sight.core.groupauth.GroupAuthorizationService
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.GroupRecordDraftService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import java.time.Instant
import java.util.Optional

@WebMvcTest(
    controllers = [com.sight.controllers.http.GroupRecordDraftController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupRecordDraftControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var drafts: GroupRecordDraftService

    @MockBean
    private lateinit var groups: GroupRepository

    @MockBean
    private lateinit var members: GroupMemberRepository

    @MockBean
    private lateinit var authorizationRepository: GroupAuthorizationRepository

    @BeforeEach
    fun setUp() {
        given(groups.findById(GROUP_ID)).willReturn(Optional.of(testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID)))
        given(members.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `그룹원이 아니면 임시저장 API를 호출할 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.USER)

        expectStatus(403)

        verifyNoInteractions(drafts)
    }

    @Test
    fun `운영진이라도 그룹원이 아니면 임시저장 API를 호출할 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.MANAGER)

        expectStatus(403)

        verifyNoInteractions(drafts)
    }

    @Test
    fun `그룹원은 임시저장 API를 호출할 수 있다`() {
        authenticate(MEMBER_ID, UserRole.USER)
        endpoints().forEach { it.reachService(MEMBER_ID) }

        expectStatus(REACHED_SERVICE)
    }

    @Test
    fun `그룹이 없으면 임시저장 API를 호출할 수 없다`() {
        given(groups.findById(GROUP_ID)).willReturn(Optional.empty())
        authenticate(MEMBER_ID, UserRole.USER)

        expectStatus(404)

        verifyNoInteractions(drafts)
    }

    @Test
    fun `인증되지 않으면 임시저장 API를 호출할 수 없다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )

        expectStatus(401)

        verifyNoInteractions(drafts)
    }

    private fun endpoints(): List<Endpoint> =
        listOf(
            Endpoint("GET /groups/$GROUP_ID/records/draft/meta", { get("/groups/$GROUP_ID/records/draft/meta") }) { requester ->
                willThrow(reached()).given(drafts).getGroupRecordDraftMetadataByMemberId(GROUP_ID, requester)
            },
            Endpoint("GET /groups/$GROUP_ID/records/draft", { get("/groups/$GROUP_ID/records/draft") }) { requester ->
                willThrow(reached()).given(drafts).getGroupRecordDraftByMemberId(GROUP_ID, requester)
            },
            Endpoint(
                "PUT /groups/$GROUP_ID/records/draft",
                {
                    put("/groups/$GROUP_ID/records/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"cardId":"$CARD_ID","recordId":null,"content":"본문","updatedAt":"$UPDATED_AT","files":[]}""")
                },
            ) { requester ->
                willThrow(reached()).given(drafts)
                    .updateGroupRecordDraft(GROUP_ID, requester, CARD_ID, null, "본문", Instant.parse(UPDATED_AT), emptyList())
            },
            Endpoint("DELETE /groups/$GROUP_ID/records/draft", { delete("/groups/$GROUP_ID/records/draft") }) { requester ->
                willThrow(reached()).given(drafts).deleteGroupRecordDraft(GROUP_ID, requester)
            },
        )

    private fun expectStatus(expected: Int) {
        endpoints().forEach { endpoint ->
            val actual = mockMvc.perform(endpoint.request()).andReturn().response.status
            assertEquals(expected, actual, endpoint.name)
        }
    }

    private fun reached() = UnprocessableEntityException("서비스가 호출되었다")

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
        private const val CARD_ID = 3L
        private const val LEADER_ID = 1L
        private const val MEMBER_ID = 2L
        private const val OUTSIDER_ID = 9L
        private const val REACHED_SERVICE = 422
        private const val UPDATED_AT = "2026-11-01T00:00:00Z"
    }
}
