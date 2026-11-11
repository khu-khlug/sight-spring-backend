package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuthAspect
import com.sight.core.groupauth.GroupAuthorizationService
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupDiscordChannel
import com.sight.domain.group.GroupStatus
import com.sight.domain.group.testGroup
import com.sight.repository.GroupAuthorizationRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.GroupDiscordChannelService
import com.sight.service.GroupInfoResult
import com.sight.service.GroupInfoService
import com.sight.service.GroupMatchingService
import com.sight.service.GroupRequesterRole
import com.sight.service.GroupService
import org.hamcrest.Matchers.nullValue
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
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
    private lateinit var groupInfoService: GroupInfoService

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
            GroupDiscordChannel(
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

    @Test
    fun `그룹 정보는 그룹을 열람할 수 있는 회원만 조회할 수 있다`() {
        givenGroup(GroupVisibility.PRIVATE)
        given(groupInfoService.getGroupInfoById(GROUP_ID, MEMBER_ID)).willReturn(groupInfoResult(GROUP_ID))

        authenticate(OUTSIDER_ID, UserRole.USER)
        mockMvc.perform(get("/groups/$GROUP_ID/info")).andExpect(status().isForbidden)

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc.perform(get("/groups/$GROUP_ID/info")).andExpect(status().isOk)
    }

    @Test
    fun `그룹 정보의 그룹 아이디는 10진 문자열, 시각은 UTC Z 형식이고 특수 조직은 null이다`() {
        val groupId = 906558544281600001L
        given(groupRepository.findById(groupId))
            .willReturn(Optional.of(testGroup(id = groupId, leaderMemberId = LEADER_ID, visibility = GroupVisibility.PUBLIC)))
        given(groupInfoService.getGroupInfoById(groupId, MEMBER_ID)).willReturn(groupInfoResult(groupId))

        authenticate(MEMBER_ID, UserRole.USER)
        mockMvc
            .perform(get("/groups/$groupId/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("906558544281600001"))
            .andExpect(jsonPath("$.createdAt").value("2026-03-02T00:00:00Z"))
            .andExpect(jsonPath("$.lastActivityAt").value("2026-09-20T09:00:00Z"))
            .andExpect(jsonPath("$.specialOrganization").value(nullValue()))
    }

    private fun groupInfoResult(id: Long): GroupInfoResult =
        GroupInfoResult(
            id = id,
            title = "테스트 그룹",
            description = null,
            category = GroupCategory.STUDY,
            status = GroupStatus.PROGRESS,
            requesterRole = GroupRequesterRole.MEMBER,
            isBookmarked = false,
            isInChatRoom = false,
            interests = null,
            techStack = null,
            repositoryUrls = null,
            memberCount = 1,
            listCount = 0,
            cardCount = 0,
            recordCount = 0,
            createdAt = Instant.parse("2026-03-02T00:00:00Z"),
            lastActivityAt = Instant.parse("2026-09-20T09:00:00Z"),
            allowJoin = true,
            visibility = GroupVisibility.PUBLIC,
            isPortfolioPublished = false,
            portfolioListCount = 0,
            portfolioCardCount = 0,
            seminarCount = 0,
            reportCount = 0,
            hasChatRoom = false,
            chatRoomUrl = null,
        )

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
