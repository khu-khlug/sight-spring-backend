package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupDiscordChannel
import com.sight.repository.GroupDiscordChannelRepository
import com.sight.repository.GroupRepository
import com.sight.service.discord.DiscordApiAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupDiscordChannelService(
    private val groupRepository: GroupRepository,
    private val groupDiscordChannelRepository: GroupDiscordChannelRepository,
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
        val discordChannelId = discordApiAdapter.createGroupTextChannel(channelName)

        val groupDiscordChannel =
            GroupDiscordChannel(
                id = UlidCreator.getUlid().toString(),
                groupId = groupId,
                discordChannelId = discordChannelId,
            )

        return groupDiscordChannelRepository.save(groupDiscordChannel)
    }

    private fun generateChannelName(groupTitle: String): String {
        return groupTitle
            .lowercase()
            .replace(Regex("[^a-z0-9가-힣\\s]"), "")
            .replace(Regex("\\s+"), "-")
            .take(100)
    }
}
