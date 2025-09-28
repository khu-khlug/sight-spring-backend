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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.willReturn
import java.util.Optional
import kotlin.test.assertEquals

class GroupDiscordChannelServiceTest {
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

    @Test
    fun `그룹장이 디스코드 채널을 생성한다`() {
        val groupId = 1L
        val masterId = 100L
        val group =
            Group(
                id = groupId,
                category = GroupCategory.PROJECT,
                title = "테스트 그룹",
                author = masterId,
                master = masterId,
                state = GroupState.PROGRESS,
                grade = GroupAccessGrade.MEMBER,
            )
        val discordChannelId = "1234567890"
        val mockTextChannel = mock<TextChannel>()
        val groupDiscordChannel =
            GroupDiscordChannel(
                id = "test-ulid",
                groupId = groupId,
                discordChannelId = discordChannelId,
            )
        val mockGroupMasterDiscordIntegration = mock<DiscordIntegration>()

        given(mockTextChannel.id).willReturn(discordChannelId)
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group))
        given(groupDiscordChannelRepository.existsByGroupId(groupId)).willReturn(false)
        given(discordIntegrationRepository.findByUserId(masterId)).willReturn(mockGroupMasterDiscordIntegration)
        given(discordApiAdapter.createGroupPrivateTextChannel("테스트-그룹")).willReturn(mockTextChannel)
        given(groupDiscordChannelRepository.save(any<GroupDiscordChannel>())).willReturn(groupDiscordChannel)

        val result = groupDiscordChannelService.createDiscordChannel(groupId, masterId)

        assertEquals(groupDiscordChannel.id, result.id)
        assertEquals(groupId, result.groupId)
        assertEquals(discordChannelId, result.discordChannelId)
        verify(discordApiAdapter).createGroupPrivateTextChannel("테스트-그룹")
        verify(groupDiscordChannelRepository).save(any<GroupDiscordChannel>())
    }

    @Test
    fun `존재하지 않는 그룹에 대해 채널 생성을 시도하면 NotFoundException이 발생한다`() {
        val groupId = 1L
        val requesterId = 100L

        given(groupRepository.findById(groupId)).willReturn(Optional.empty())

        assertThrows<NotFoundException> {
            groupDiscordChannelService.createDiscordChannel(groupId, requesterId)
        }
    }

    @Test
    fun `그룹장이 아닌 사용자가 채널 생성을 시도하면 ForbiddenException이 발생한다`() {
        val groupId = 1L
        val masterId = 100L
        val nonMasterId = 200L
        val group =
            Group(
                id = groupId,
                category = GroupCategory.PROJECT,
                title = "테스트 그룹",
                author = masterId,
                master = masterId,
                state = GroupState.PROGRESS,
                grade = GroupAccessGrade.MEMBER,
            )

        given(groupRepository.findById(groupId)).willReturn(Optional.of(group))

        assertThrows<ForbiddenException> {
            groupDiscordChannelService.createDiscordChannel(groupId, nonMasterId)
        }
    }

    @Test
    fun `이미 디스코드 채널이 존재하는 그룹에 대해 채널 생성을 시도하면 422 에러가 발생한다`() {
        val groupId = 1L
        val masterId = 100L
        val group =
            Group(
                id = groupId,
                category = GroupCategory.PROJECT,
                title = "테스트 그룹",
                author = masterId,
                master = masterId,
                state = GroupState.PROGRESS,
                grade = GroupAccessGrade.MEMBER,
            )

        given(groupRepository.findById(groupId)).willReturn(Optional.of(group))
        given(groupDiscordChannelRepository.existsByGroupId(groupId)).willReturn(true)

        assertThrows<UnprocessableEntityException> {
            groupDiscordChannelService.createDiscordChannel(groupId, masterId)
        }
    }
}
