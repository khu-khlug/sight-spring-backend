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
import com.sight.service.GroupRecordArchiveService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@WebMvcTest(
    controllers = [com.sight.controllers.http.GroupRecordArchiveController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupRecordArchiveControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var archives: GroupRecordArchiveService

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
    fun `그룹원이 아니면 기록 보관 여부를 바꿀 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.USER)

        mockMvc.perform(request()).andExpect(status().isForbidden)

        verifyNoInteractions(archives)
    }

    @Test
    fun `운영진이라도 그룹원이 아니면 기록 보관 여부를 바꿀 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.MANAGER)

        mockMvc.perform(request()).andExpect(status().isForbidden)

        verifyNoInteractions(archives)
    }

    @Test
    fun `그룹원은 기록 보관 여부를 바꿀 수 있다`() {
        authenticate(MEMBER_ID, UserRole.USER)

        mockMvc.perform(request()).andExpect(status().isNoContent)

        verify(archives).updateGroupRecordArchive(GROUP_ID, MEMBER_ID, RECORD_ID, true)
    }

    @Test
    fun `그룹이 없으면 기록 보관 여부를 바꿀 수 없다`() {
        given(groups.findById(GROUP_ID)).willReturn(Optional.empty())
        authenticate(MEMBER_ID, UserRole.USER)

        mockMvc.perform(request()).andExpect(status().isNotFound)

        verifyNoInteractions(archives)
    }

    @Test
    fun `인증되지 않으면 기록 보관 여부를 바꿀 수 없다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )

        mockMvc.perform(request()).andExpect(status().isUnauthorized)

        verifyNoInteractions(archives)
    }

    private fun request() =
        put("/groups/$GROUP_ID/records/$RECORD_ID/archive").contentType(MediaType.APPLICATION_JSON).content("""{"isInUserArchive":true}""")

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

    companion object {
        private const val GROUP_ID = 1L
        private const val RECORD_ID = 4L
        private const val LEADER_ID = 1L
        private const val MEMBER_ID = 2L
        private const val OUTSIDER_ID = 9L
    }
}
