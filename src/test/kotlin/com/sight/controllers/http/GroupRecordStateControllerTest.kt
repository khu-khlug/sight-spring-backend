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
import com.sight.service.GroupRecordStateService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import java.util.Optional

@WebMvcTest(
    controllers = [com.sight.controllers.http.GroupRecordStateController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupRecordStateControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var states: GroupRecordStateService

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
    fun `그룹원이 아니면 기록 상태 변경 API를 호출할 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.USER)

        expectStatus(403)

        verifyNoInteractions(states)
    }

    @Test
    fun `운영진이라도 그룹원이 아니면 기록 상태 변경 API를 호출할 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.MANAGER)

        expectStatus(403)

        verifyNoInteractions(states)
    }

    @Test
    fun `그룹원은 기록 상태 변경 API를 호출할 수 있다`() {
        authenticate(MEMBER_ID, UserRole.USER)
        endpoints().forEach { it.reachService(MEMBER_ID) }

        expectStatus(REACHED_SERVICE)
    }

    @Test
    fun `그룹이 없으면 기록 상태 변경 API를 호출할 수 없다`() {
        given(groups.findById(GROUP_ID)).willReturn(Optional.empty())
        authenticate(MEMBER_ID, UserRole.USER)

        expectStatus(404)

        verifyNoInteractions(states)
    }

    @Test
    fun `인증되지 않으면 기록 상태 변경 API를 호출할 수 없다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )

        expectStatus(401)

        verifyNoInteractions(states)
    }

    private fun endpoints(): List<Endpoint> =
        listOf(
            Endpoint("DELETE /groups/$GROUP_ID/records/$RECORD_ID", { delete("/groups/$GROUP_ID/records/$RECORD_ID") }) { requester ->
                willThrow(reached()).given(states).deleteGroupRecord(GROUP_ID, requester, RECORD_ID)
            },
            Endpoint(
                "POST /groups/$GROUP_ID/records/$RECORD_ID/restore",
                { post("/groups/$GROUP_ID/records/$RECORD_ID/restore") },
            ) { requester ->
                willThrow(reached()).given(states).updateGroupRecordRestored(GROUP_ID, requester, RECORD_ID)
            },
            Endpoint(
                "PUT /groups/$GROUP_ID/records/$RECORD_ID/position",
                {
                    put("/groups/$GROUP_ID/records/$RECORD_ID/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"targetCardId":"$TARGET_CARD_ID"}""")
                },
            ) { requester ->
                willThrow(reached()).given(states).updateGroupRecordPosition(GROUP_ID, requester, RECORD_ID, TARGET_CARD_ID)
            },
            Endpoint(
                "PUT /groups/$GROUP_ID/records/$RECORD_ID/portfolio",
                {
                    put("/groups/$GROUP_ID/records/$RECORD_ID/portfolio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"isInPortfolio":true}""")
                },
            ) { requester ->
                willThrow(reached()).given(states).updateGroupRecordPortfolio(GROUP_ID, requester, RECORD_ID, true)
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
        private const val RECORD_ID = 4L
        private const val TARGET_CARD_ID = 5L
        private const val LEADER_ID = 1L
        private const val MEMBER_ID = 2L
        private const val OUTSIDER_ID = 9L
        private const val REACHED_SERVICE = 422
    }
}
