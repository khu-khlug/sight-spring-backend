package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuthAspect
import com.sight.core.groupauth.GroupAuthorizationService
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.GroupFileDownloadService
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
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@WebMvcTest(
    controllers = [com.sight.controllers.http.GroupFileController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupFileControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var files: GroupFileDownloadService

    @MockBean
    private lateinit var groups: GroupRepository

    @MockBean
    private lateinit var members: GroupMemberRepository

    @MockBean
    private lateinit var authorizationRepository: GroupAuthorizationRepository

    @BeforeEach
    fun setUp() {
        givenGroup(GroupVisibility.PRIVATE)
        given(members.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)
        given(files.getGroupFileUrl(GROUP_ID, FILE_ID)).willReturn(FILE_URL)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `비공개 그룹에서는 그룹원이 아니면 파일을 조회할 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.USER)

        mockMvc.perform(get("/groups/$GROUP_ID/files/$FILE_ID")).andExpect(status().isForbidden)

        verifyNoInteractions(files)
    }

    @Test
    fun `운영진이라도 비공개 그룹의 그룹원이 아니면 파일을 조회할 수 없다`() {
        authenticate(OUTSIDER_ID, UserRole.MANAGER)

        mockMvc.perform(get("/groups/$GROUP_ID/files/$FILE_ID")).andExpect(status().isForbidden)

        verifyNoInteractions(files)
    }

    @Test
    fun `비공개 그룹의 그룹원은 파일을 조회할 수 있다`() {
        authenticate(MEMBER_ID, UserRole.USER)

        mockMvc.perform(get("/groups/$GROUP_ID/files/$FILE_ID")).andExpect(status().isFound)
    }

    @Test
    fun `완전 공개 그룹에서는 그룹원이 아니어도 파일을 조회할 수 있다`() {
        givenGroup(GroupVisibility.PUBLIC)
        authenticate(OUTSIDER_ID, UserRole.USER)

        mockMvc.perform(get("/groups/$GROUP_ID/files/$FILE_ID")).andExpect(status().isFound)
    }

    @Test
    fun `그룹이 없으면 파일을 조회할 수 없다`() {
        given(groups.findById(GROUP_ID)).willReturn(Optional.empty())
        authenticate(MEMBER_ID, UserRole.USER)

        mockMvc.perform(get("/groups/$GROUP_ID/files/$FILE_ID")).andExpect(status().isNotFound)

        verifyNoInteractions(files)
    }

    @Test
    fun `인증되지 않으면 파일을 조회할 수 없다`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")),
            )

        mockMvc.perform(get("/groups/$GROUP_ID/files/$FILE_ID")).andExpect(status().isUnauthorized)

        verifyNoInteractions(files)
    }

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

    companion object {
        private const val GROUP_ID = 1L
        private const val LEADER_ID = 1L
        private const val MEMBER_ID = 2L
        private const val OUTSIDER_ID = 9L
        private const val FILE_ID = "file-1"
        private const val FILE_URL = "https://files.example/file-1"
    }
}
