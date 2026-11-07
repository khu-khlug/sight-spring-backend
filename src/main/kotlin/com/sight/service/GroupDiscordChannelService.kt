package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupDiscordChannel
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.GroupDiscordChannelRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.service.discord.DiscordApiAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

// 그룹 Discord 채널 API가 유지하는 응답 형식. 생성을 완료한 채널만 표현한다.
data class GroupDiscordChannelInfo(
    val id: String,
    val groupId: Long,
    val discordChannelId: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

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
    ): GroupDiscordChannelInfo {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        if (groupDiscordChannelRepository.findByGroupId(groupId) != null) {
            throw UnprocessableEntityException("이미 디스코드 채널이 존재합니다")
        }

        val groupMasterDiscordIntegration =
            discordIntegrationRepository.findByUserId(requesterId)
                ?: throw UnprocessableEntityException("요청자의 디스코드 연동 정보가 존재하지 않아 디스코드 채널을 만들 수 없습니다. 디스코드 연동을 먼저 진행해주세요.")

        val groupDiscordChannel =
            GroupDiscordChannel(
                id = UlidCreator.getUlid().toString(),
                groupId = groupId,
                createdAt = Instant.now(),
            )

        val channelName = generateChannelName(group.title)
        val discordChannel = discordApiAdapter.createGroupPrivateTextChannel(channelName)
        groupDiscordChannel.attachChannel(discordChannel.id)

        discordApiAdapter.addMemberToChannel(discordChannel.id, groupMasterDiscordIntegration.discordUserId)
        groupDiscordChannel.complete(Instant.now())

        return groupDiscordChannelRepository.save(groupDiscordChannel).toInfo()
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

        val discordChannelId =
            findCreatedDiscordChannel(groupId)?.discordChannelId
                ?: throw NotFoundException("해당 그룹의 디스코드 채널이 아직 만들어지지 않았습니다. 디스코드 채널을 생성하고 다시 시도해주세요.")

        validateMemberAddPermission(group, memberId, requesterId)

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw ForbiddenException("해당 멤버가 그룹에 속하지 않았습니다")
        }

        val discordIntegration =
            discordIntegrationRepository.findByUserId(memberId)
                ?: throw UnprocessableEntityException("해당 멤버의 디스코드 연동 정보가 존재하지 않습니다")

        discordApiAdapter.addMemberToChannel(discordChannelId, discordIntegration.discordUserId)
    }

    @Transactional(readOnly = true)
    fun getDiscordChannelByGroupId(groupId: Long): GroupDiscordChannelInfo {
        return findCreatedDiscordChannel(groupId)?.toInfo()
            ?: throw NotFoundException("해당 그룹의 디스코드 채널이 존재하지 않습니다")
    }

    @Transactional(readOnly = true)
    fun checkUserInDiscordChannel(
        groupId: Long,
        userId: Long,
    ): Boolean {
        val discordChannelId =
            findCreatedDiscordChannel(groupId)?.discordChannelId
                ?: throw NotFoundException("해당 그룹이 아직 디스코드 채널을 생성하지 않았습니다.")

        val discordIntegration =
            discordIntegrationRepository.findByUserId(userId)
                ?: return false

        return discordApiAdapter.isUserInChannel(discordChannelId, discordIntegration.discordUserId)
    }

    // 생성을 완료해 Discord 채널이 연결된 행만 사용한다.
    private fun findCreatedDiscordChannel(groupId: Long): GroupDiscordChannel? =
        groupDiscordChannelRepository.findByGroupId(groupId)?.takeIf { it.isCreated }

    private fun validateMemberAddPermission(
        group: Group,
        memberId: Long,
        requesterId: Long,
    ) {
        if (group.isLeader(requesterId)) {
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

    private fun GroupDiscordChannel.toInfo(): GroupDiscordChannelInfo {
        val createdAt = LocalDateTime.ofInstant(this.createdAt, ZoneOffset.UTC)
        return GroupDiscordChannelInfo(
            id = id,
            groupId = groupId,
            discordChannelId = checkNotNull(discordChannelId) { "Discord 채널이 연결되지 않았습니다." },
            createdAt = createdAt,
            updatedAt = completedAt?.let { LocalDateTime.ofInstant(it, ZoneOffset.UTC) } ?: createdAt,
        )
    }
}
