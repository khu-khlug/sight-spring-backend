package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuthAspect
import com.sight.core.groupauth.GroupAuthorizationService
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.GroupMemberListItem
import com.sight.service.GroupMemberListResult
import com.sight.service.GroupMemberService
import org.junit.jupiter.api.AfterEach
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Optional

@WebMvcTest(
    GroupMemberController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupMemberControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var groupMemberService: GroupMemberService

    @MockBean
    private lateinit var groupRepository: GroupRepository

    @MockBean
    private lateinit var groupMemberRepository: GroupMemberRepository

    @MockBean
    private lateinit var groupAuthorizationRepository: GroupAuthorizationRepository

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `그룹원 목록은 그룹을 열람할 수 있는 회원만 조회할 수 있고 응답 필드를 포함한다`() {
        givenGroup(visibility = GroupVisibility.PRIVATE)
        given(groupMemberService.listGroupMembers(GROUP_ID)).willReturn(
            GroupMemberListResult(
                leaderUserId = LEADER_ID,
                members =
                    listOf(
                        GroupMemberListItem(
                            userId = LEADER_ID,
                            name = "홍길동",
                            college = "공과대학",
                            cardCount = 3L,
                            recordCount = 7L,
                        ),
                    ),
            ),
        )

        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(get("/groups/$GROUP_ID/members")).andExpect(status().isForbidden)

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(get("/groups/$GROUP_ID/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.leaderUserId").value(LEADER_ID))
            .andExpect(jsonPath("$.members[0].userId").value(LEADER_ID))
            .andExpect(jsonPath("$.members[0].name").value("홍길동"))
            .andExpect(jsonPath("$.members[0].college").value("공과대학"))
            .andExpect(jsonPath("$.members[0].cardCount").value(3))
            .andExpect(jsonPath("$.members[0].recordCount").value(7))
    }

    @Test
    fun `그룹장 위임은 그룹장만 할 수 있다`() {
        givenGroup()

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(delegateRequest()).andExpect(status().isForbidden)

        authenticate(LEADER_ID, UserRole.USER)
        mockMvc.perform(delegateRequest()).andExpect(status().isNoContent)
    }

    @Test
    fun `그룹 참가는 그룹을 열람할 수 있는 회원만 할 수 있다`() {
        givenGroup(visibility = GroupVisibility.PRIVATE)
        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(post("/groups/$GROUP_ID/members/@me")).andExpect(status().isForbidden)

        givenGroup(visibility = GroupVisibility.PUBLIC)
        mockMvc.perform(post("/groups/$GROUP_ID/members/@me")).andExpect(status().isCreated)
    }

    @Test
    fun `일반 그룹의 그룹원 내보내기는 그룹장만 할 수 있다`() {
        givenGroup()

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(delete("/groups/$GROUP_ID/members/$TARGET_ID")).andExpect(status().isForbidden)

        authenticate(MANAGER_ID, UserRole.MANAGER)
        mockMvc.perform(delete("/groups/$GROUP_ID/members/$TARGET_ID")).andExpect(status().isForbidden)

        authenticate(LEADER_ID, UserRole.USER)
        mockMvc.perform(delete("/groups/$GROUP_ID/members/$TARGET_ID")).andExpect(status().isNoContent)
    }

    @Test
    fun `운영진 공개 운영 그룹에서는 그룹장이 아닌 운영진도 그룹원을 내보낼 수 있다`() {
        givenGroup(category = GroupCategory.MANAGE, visibility = GroupVisibility.MANAGER_ONLY)
        given(groupAuthorizationRepository.isManageGroup(GROUP_ID)).willReturn(true)

        authenticate(MANAGER_ID, UserRole.MANAGER)
        mockMvc.perform(delete("/groups/$GROUP_ID/members/$TARGET_ID")).andExpect(status().isNoContent)
    }

    @Test
    fun `그룹 나가기는 그룹원만 할 수 있다`() {
        givenGroup()

        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(delete("/groups/$GROUP_ID/members/@me")).andExpect(status().isForbidden)

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(delete("/groups/$GROUP_ID/members/@me")).andExpect(status().isNoContent)
    }

    private fun delegateRequest() =
        patch("/groups/$GROUP_ID/master")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"memberId":$MEMBER_ID}""")

    private fun givenGroup(
        category: GroupCategory = GroupCategory.STUDY,
        visibility: GroupVisibility = GroupVisibility.MEMBER_ONLY,
    ) {
        given(groupRepository.findById(GROUP_ID)).willReturn(
            Optional.of(testGroup(id = GROUP_ID, category = category, leaderMemberId = LEADER_ID, visibility = visibility)),
        )
        given(groupMemberRepository.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)
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
        private const val TARGET_ID = 5L
        private const val MANAGER_ID = 9L
    }
}
