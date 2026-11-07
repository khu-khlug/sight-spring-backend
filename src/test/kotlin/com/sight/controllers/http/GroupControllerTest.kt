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
import com.sight.service.GroupDiscordChannelInfo
import com.sight.service.GroupDiscordChannelService
import com.sight.service.GroupMatchingService
import com.sight.service.GroupService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(
    GroupController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class, GroupAuthAspect::class, GroupAuthorizationService::class)
@EnableAspectJAutoProxy
class GroupControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var groupDiscordChannelService: GroupDiscordChannelService

    @MockBean
    private lateinit var groupMatchingService: GroupMatchingService

    @MockBean
    private lateinit var groupService: GroupService

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
    fun `디스코드 채널은 그룹장만 만들 수 있다`() {
        givenGroup(GroupVisibility.MEMBER_ONLY)
        given(groupDiscordChannelService.createDiscordChannel(GROUP_ID, LEADER_ID)).willReturn(
            GroupDiscordChannelInfo(
                id = "channel-1",
                groupId = GROUP_ID,
                discordChannelId = "1234567890",
                createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            ),
        )

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(post("/groups/$GROUP_ID/discord-channel")).andExpect(status().isForbidden)

        authenticate(MANAGER_ID, UserRole.MANAGER)
        mockMvc.perform(post("/groups/$GROUP_ID/discord-channel")).andExpect(status().isForbidden)

        authenticate(LEADER_ID, UserRole.USER)
        mockMvc.perform(post("/groups/$GROUP_ID/discord-channel")).andExpect(status().isOk)
    }

    @Test
    fun `북마크는 그룹을 열람할 수 있는 회원만 추가하고 취소할 수 있다`() {
        givenGroup(GroupVisibility.PRIVATE)

        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(post("/groups/$GROUP_ID/bookmark")).andExpect(status().isForbidden)
        mockMvc.perform(delete("/groups/$GROUP_ID/bookmark")).andExpect(status().isForbidden)

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(post("/groups/$GROUP_ID/bookmark")).andExpect(status().isCreated)
        mockMvc.perform(delete("/groups/$GROUP_ID/bookmark")).andExpect(status().isNoContent)
    }

    @Test
    fun `포트폴리오는 그룹장만 발행하고 취소할 수 있다`() {
        givenGroup(GroupVisibility.MEMBER_ONLY)

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(post("/groups/$GROUP_ID/portfolio")).andExpect(status().isForbidden)
        mockMvc.perform(delete("/groups/$GROUP_ID/portfolio")).andExpect(status().isForbidden)

        authenticate(MANAGER_ID, UserRole.MANAGER)
        mockMvc.perform(post("/groups/$GROUP_ID/portfolio")).andExpect(status().isForbidden)
        mockMvc.perform(delete("/groups/$GROUP_ID/portfolio")).andExpect(status().isForbidden)

        authenticate(LEADER_ID, UserRole.USER)
        mockMvc.perform(post("/groups/$GROUP_ID/portfolio")).andExpect(status().isOk)
        mockMvc.perform(delete("/groups/$GROUP_ID/portfolio")).andExpect(status().isNoContent)
    }

    private fun givenGroup(visibility: GroupVisibility) {
        given(groupRepository.findById(GROUP_ID))
            .willReturn(Optional.of(testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID, visibility = visibility)))
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
        private const val MANAGER_ID = 9L
    }
}
