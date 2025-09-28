package com.sight.service

import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.discord.DiscordIntegration
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupDiscordChannel
import com.sight.domain.group.GroupState
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.GroupDiscordChannelRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.discord.DiscordApiAdapter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.Optional

class GroupDiscordChannelServiceMemberTest {
    private val groupRepository = mock<GroupRepository>()
    private val groupDiscordChannelRepository = mock<GroupDiscordChannelRepository>()
    private val groupMemberRepository = mock<GroupMemberRepository>()
    private val discordIntegrationRepository = mock<DiscordIntegrationRepository>()
    private val discordApiAdapter = mock<DiscordApiAdapter>()
    private val groupDiscordChannelService =
        GroupDiscordChannelService(
            groupRepository,
            groupDiscordChannelRepository,
            groupMemberRepository,
            discordIntegrationRepository,
            discordApiAdapter,
        )

    private val groupId = 1L
    private val masterId = 100L
    private val memberId = 200L
    private val otherId = 300L
    private val discordChannelId = "test-channel-id"
    private val discordUserId = "test-discord-user-id"

    private val testGroup =
        Group(
            id = groupId,
            category = GroupCategory.STUDY,
            title = "테스트 그룹",
            author = masterId,
            master = masterId,
            state = GroupState.PROGRESS,
            allowJoin = true,
            grade = GroupAccessGrade.MEMBER,
            countMember = 1L,
            countList = 0L,
            countCard = 0L,
            countRecord = 0L,
            portfolio = false,
        )

    private val testGroupDiscordChannel =
        GroupDiscordChannel(
            id = "test-id",
            groupId = groupId,
            discordChannelId = discordChannelId,
        )

    private val testDiscordIntegration =
        DiscordIntegration(
            id = "integration-id",
            userId = memberId,
            discordUserId = discordUserId,
            createdAt = LocalDateTime.now(),
        )

    @Test
    fun `그룹장이 그룹 멤버를 디스코드 채널에 추가한다`() {
        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup))
        given(groupDiscordChannelRepository.findByGroupId(groupId)).willReturn(testGroupDiscordChannel)
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(true)
        given(discordIntegrationRepository.findByUserId(memberId)).willReturn(testDiscordIntegration)

        groupDiscordChannelService.addMemberToDiscordChannel(
            groupId = groupId,
            memberId = memberId,
            requesterId = masterId,
        )

        verify(discordApiAdapter).addMemberToChannel(discordChannelId, discordUserId)
    }

    @Test
    fun `일반 멤버가 자신을 디스코드 채널에 추가한다`() {
        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup))
        given(groupDiscordChannelRepository.findByGroupId(groupId)).willReturn(testGroupDiscordChannel)
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(true)
        given(discordIntegrationRepository.findByUserId(memberId)).willReturn(testDiscordIntegration)

        groupDiscordChannelService.addMemberToDiscordChannel(
            groupId = groupId,
            memberId = memberId,
            requesterId = memberId,
        )

        verify(discordApiAdapter).addMemberToChannel(discordChannelId, discordUserId)
    }

    @Test
    fun `존재하지 않는 그룹에 멤버를 추가하려 하면 예외가 발생한다`() {
        given(groupRepository.findById(groupId)).willReturn(Optional.empty())

        assertThrows<NotFoundException> {
            groupDiscordChannelService.addMemberToDiscordChannel(
                groupId = groupId,
                memberId = memberId,
                requesterId = masterId,
            )
        }
    }

    @Test
    fun `디스코드 채널이 없는 그룹에 멤버를 추가하려 하면 예외가 발생한다`() {
        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup))
        given(groupDiscordChannelRepository.findByGroupId(groupId)).willReturn(null)

        assertThrows<NotFoundException> {
            groupDiscordChannelService.addMemberToDiscordChannel(
                groupId = groupId,
                memberId = memberId,
                requesterId = masterId,
            )
        }
    }

    @Test
    fun `일반 멤버가 다른 멤버를 추가하려 하면 예외가 발생한다`() {
        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup))
        given(groupDiscordChannelRepository.findByGroupId(groupId)).willReturn(testGroupDiscordChannel)

        assertThrows<ForbiddenException> {
            groupDiscordChannelService.addMemberToDiscordChannel(
                groupId = groupId,
                memberId = otherId,
                requesterId = memberId,
            )
        }
    }

    @Test
    fun `그룹에 속하지 않은 멤버를 추가하려 하면 예외가 발생한다`() {
        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup))
        given(groupDiscordChannelRepository.findByGroupId(groupId)).willReturn(testGroupDiscordChannel)
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, otherId)).willReturn(false)

        assertThrows<ForbiddenException> {
            groupDiscordChannelService.addMemberToDiscordChannel(
                groupId = groupId,
                memberId = otherId,
                requesterId = masterId,
            )
        }
    }

    @Test
    fun `디스코드 연동 정보가 없는 멤버를 추가하려 하면 예외가 발생한다`() {
        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup))
        given(groupDiscordChannelRepository.findByGroupId(groupId)).willReturn(testGroupDiscordChannel)
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).willReturn(true)
        given(discordIntegrationRepository.findByUserId(memberId)).willReturn(null)

        assertThrows<UnprocessableEntityException> {
            groupDiscordChannelService.addMemberToDiscordChannel(
                groupId = groupId,
                memberId = memberId,
                requesterId = masterId,
            )
        }
    }

    @Test
    fun `그룹에 속하지 않은 요청자가 자신을 추가하려 하면 예외가 발생한다`() {
        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup))
        given(groupDiscordChannelRepository.findByGroupId(groupId)).willReturn(testGroupDiscordChannel)
        given(groupMemberRepository.existsByGroupIdAndMemberId(groupId, otherId)).willReturn(false)

        assertThrows<ForbiddenException> {
            groupDiscordChannelService.addMemberToDiscordChannel(
                groupId = groupId,
                memberId = otherId,
                requesterId = otherId,
            )
        }
    }
}
