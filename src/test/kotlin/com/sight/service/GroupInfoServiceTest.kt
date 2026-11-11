package com.sight.service

import com.sight.core.exception.InternalServerErrorException
import com.sight.domain.discord.DiscordIntegration
import com.sight.domain.group.Group
import com.sight.domain.group.setField
import com.sight.domain.group.testGroup
import com.sight.domain.group.testGroupDiscordChannel
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.GroupActivityReportRepository
import com.sight.repository.GroupBookmarkRepository
import com.sight.repository.GroupDiscordChannelRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupPortfolioRepository
import com.sight.repository.GroupRepository
import com.sight.repository.projection.GroupActivityReportCountProjection
import com.sight.repository.projection.GroupPortfolioCountProjection
import com.sight.service.discord.DiscordApiAdapter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupInfoServiceTest {
    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var groupDiscordChannelRepository: GroupDiscordChannelRepository
    private lateinit var discordIntegrationRepository: DiscordIntegrationRepository
    private lateinit var discordApiAdapter: DiscordApiAdapter
    private lateinit var groupInfoService: GroupInfoService

    @BeforeEach
    fun setUp() {
        groupRepository = mock()
        groupMemberRepository = mock()
        groupDiscordChannelRepository = mock()
        discordIntegrationRepository = mock()
        discordApiAdapter = mock()
        val groupPortfolioRepository = mock<GroupPortfolioRepository>()
        given(groupPortfolioRepository.countPortfolioCardsAndListsByGroupId(GROUP_ID)).willReturn(
            object : GroupPortfolioCountProjection {
                override val cardCount = 0L
                override val listCount = 0L
            },
        )
        val groupActivityReportRepository = mock<GroupActivityReportRepository>()
        given(groupActivityReportRepository.countPresentationsAndReportsByGroupId(GROUP_ID)).willReturn(
            object : GroupActivityReportCountProjection {
                override val presentationCount = 0L
                override val reportCount = 0L
            },
        )
        groupInfoService =
            GroupInfoService(
                groupRepository = groupRepository,
                groupMemberRepository = groupMemberRepository,
                groupBookmarkRepository = mock<GroupBookmarkRepository>(),
                groupPortfolioRepository = groupPortfolioRepository,
                groupActivityReportRepository = groupActivityReportRepository,
                groupDiscordChannelRepository = groupDiscordChannelRepository,
                discordIntegrationRepository = discordIntegrationRepository,
                discordApiAdapter = discordApiAdapter,
                discordGuildId = GUILD_ID,
            )
    }

    @Test
    fun `요청자의 그룹원 여부는 그룹장 그룹원 그 밖으로 나뉜다`() {
        givenGroup(testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID))
        given(groupMemberRepository.existsByGroupIdAndMemberId(GROUP_ID, LEADER_ID)).willReturn(true)
        given(groupMemberRepository.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)

        assertEquals(GroupRequesterRole.LEADER, groupInfoService.getGroupInfoById(GROUP_ID, LEADER_ID).requesterRole)
        assertEquals(GroupRequesterRole.MEMBER, groupInfoService.getGroupInfoById(GROUP_ID, MEMBER_ID).requesterRole)
        assertEquals(GroupRequesterRole.NONE, groupInfoService.getGroupInfoById(GROUP_ID, OUTSIDER_ID).requesterRole)
    }

    @Test
    fun `채팅방이 없거나 그룹원이 아니면 채팅방 참여 여부는 false다`() {
        givenGroup(testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID))
        given(groupMemberRepository.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)

        val withoutChatRoom = groupInfoService.getGroupInfoById(GROUP_ID, MEMBER_ID)
        assertEquals(false, withoutChatRoom.isInChatRoom)
        assertEquals(false, withoutChatRoom.hasChatRoom)
        assertNull(withoutChatRoom.chatRoomUrl)

        givenChatRoom()
        assertEquals(false, groupInfoService.getGroupInfoById(GROUP_ID, OUTSIDER_ID).isInChatRoom)
    }

    @Test
    fun `Discord 조회에 실패해도 그룹 정보를 반환하고 채팅방 참여 여부만 null이다`() {
        givenGroup(testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID))
        given(groupMemberRepository.existsByGroupIdAndMemberId(GROUP_ID, MEMBER_ID)).willReturn(true)
        givenChatRoom()
        given(discordIntegrationRepository.findByUserId(MEMBER_ID)).willReturn(
            DiscordIntegration(
                id = "integration-id",
                userId = MEMBER_ID,
                discordUserId = DISCORD_USER_ID,
                createdAt = LocalDateTime.now(),
            ),
        )
        given(discordApiAdapter.isUserInChannel(CHANNEL_ID, DISCORD_USER_ID))
            .willThrow(InternalServerErrorException("디스코드 채널 참여 여부를 확인하는데 실패했습니다."))

        val result = groupInfoService.getGroupInfoById(GROUP_ID, MEMBER_ID)

        assertNull(result.isInChatRoom)
        assertEquals(true, result.hasChatRoom)
    }

    @Test
    fun `채팅방이 있으면 Discord 서버와 채널 아이디로 만든 웹 URL을 반환한다`() {
        givenGroup(testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID))
        givenChatRoom()

        val result = groupInfoService.getGroupInfoById(GROUP_ID, OUTSIDER_ID)

        assertEquals("https://discord.com/channels/$GUILD_ID/$CHANNEL_ID", result.chatRoomUrl)
    }

    @Test
    fun `사용 기술과 저장소 URL은 저장값이 없거나 빈 문자열이면 null이다`() {
        val withEmptyValues = testGroup(id = GROUP_ID, leaderMemberId = LEADER_ID)
        setField(withEmptyValues, "techStack", "")
        setField(withEmptyValues, "repositoryUrls", "")
        givenGroup(withEmptyValues)

        val result = groupInfoService.getGroupInfoById(GROUP_ID, OUTSIDER_ID)

        assertNull(result.techStack)
        assertNull(result.repositoryUrls)
    }

    private fun givenGroup(group: Group) {
        given(groupRepository.findById(GROUP_ID)).willReturn(Optional.of(group))
    }

    private fun givenChatRoom() {
        given(groupDiscordChannelRepository.findByGroupId(GROUP_ID))
            .willReturn(testGroupDiscordChannel(groupId = GROUP_ID, discordChannelId = CHANNEL_ID))
    }

    companion object {
        private const val GROUP_ID = 1L
        private const val LEADER_ID = 1L
        private const val MEMBER_ID = 2L
        private const val OUTSIDER_ID = 3L
        private const val GUILD_ID = "guild-id"
        private const val CHANNEL_ID = "channel-id"
        private const val DISCORD_USER_ID = "discord-user-id"
    }
}
