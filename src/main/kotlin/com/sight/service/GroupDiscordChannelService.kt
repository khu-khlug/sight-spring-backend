package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupDiscordChannel
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.GroupDiscordChannelRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.discord.DiscordApiAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupDiscordChannelService(
    private val groupRepository: GroupRepository,
    private val groupDiscordChannelRepository: GroupDiscordChannelRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val discordApiAdapter: DiscordApiAdapter,
) {
    @Transactional
    fun createDiscordChannel(
        groupId: Long,
        requesterId: Long,
    ): GroupDiscordChannel {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        if (group.master != requesterId) {
            throw ForbiddenException("그룹장만 디스코드 채널을 생성할 수 있습니다")
        }

        if (groupDiscordChannelRepository.existsByGroupId(groupId)) {
            throw UnprocessableEntityException("이미 디스코드 채널이 존재합니다")
        }

        val channelName = generateChannelName(group.title)
        val discordChannel = discordApiAdapter.createGroupPrivateTextChannel(channelName)

        val groupDiscordChannel =
            GroupDiscordChannel(
                id = UlidCreator.getUlid().toString(),
                groupId = groupId,
                discordChannelId = discordChannel.id,
            )

        return groupDiscordChannelRepository.save(groupDiscordChannel)
    }

    @Transactional
    fun addMemberToDiscordChannel(
        groupId: Long,
        memberId: Long,
        requesterId: Long,
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹이 존재하지 않습니다")
            }

        val groupDiscordChannel =
            groupDiscordChannelRepository.findByGroupId(groupId)
                ?: throw NotFoundException("해당 그룹의 디스코드 채널이 아직 만들어지지 않았습니다. 디스코드 채널을 생성하고 다시 시도해주세요.")

        validateMemberAddPermission(group, memberId, requesterId)

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw ForbiddenException("해당 멤버가 그룹에 속하지 않았습니다")
        }

        val discordIntegration =
            discordIntegrationRepository.findByUserId(memberId)
                ?: throw UnprocessableEntityException("해당 멤버의 디스코드 연동 정보가 존재하지 않습니다")

        discordApiAdapter.addMemberToChannel(
            groupDiscordChannel.discordChannelId,
            discordIntegration.discordUserId,
        )
    }

    private fun validateMemberAddPermission(
        group: com.sight.domain.group.Group,
        memberId: Long,
        requesterId: Long,
    ) {
        if (group.master == requesterId) {
            return
        }

        val isMemberJoinedGroup = groupMemberRepository.existsByGroupIdAndMemberId(group.id, requesterId)
        if (requesterId == memberId && isMemberJoinedGroup) {
            return
        }

        throw ForbiddenException("그룹장만 다른 멤버를 초대할 수 있으며, 일반 멤버는 자신만 추가할 수 있습니다")
    }

    private fun generateChannelName(groupTitle: String): String {
        return groupTitle
            .lowercase()
            .replace(Regex("[^a-z0-9가-힣\\s]"), "")
            .replace(Regex("\\s+"), "-")
            .take(100)
    }
}
