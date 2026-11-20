package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupInterest
import com.sight.domain.group.GroupStatus
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.GroupActivityReportRepository
import com.sight.repository.GroupBookmarkRepository
import com.sight.repository.GroupDiscordChannelRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupPortfolioRepository
import com.sight.repository.GroupRepository
import com.sight.service.discord.DiscordApiAdapter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

enum class GroupRequesterRole {
    NONE,
    MEMBER,
    LEADER,
}

data class GroupInfoResult(
    val id: Long,
    val title: String,
    val description: String?,
    val category: GroupCategory,
    val status: GroupStatus,
    val requesterRole: GroupRequesterRole,
    val isBookmarked: Boolean,
    // 채팅방이 없거나 그룹원이 아니면 false, Discord 조회에 실패하면 null이다.
    val isInChatRoom: Boolean?,
    val interests: List<GroupInterest>?,
    val techStack: List<String>?,
    val repositoryUrls: List<String>?,
    val memberCount: Long,
    val listCount: Long,
    val cardCount: Long,
    val recordCount: Long,
    val createdAt: Instant,
    val lastActivityAt: Instant,
    val allowJoin: Boolean,
    val visibility: GroupVisibility,
    val isPortfolioPublished: Boolean,
    val portfolioListCount: Long,
    val portfolioCardCount: Long,
    val seminarCount: Long,
    val reportCount: Long,
    val hasChatRoom: Boolean,
    val chatRoomUrl: String?,
)

@Service
class GroupInfoService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupBookmarkRepository: GroupBookmarkRepository,
    private val groupPortfolioRepository: GroupPortfolioRepository,
    private val groupActivityReportRepository: GroupActivityReportRepository,
    private val groupDiscordChannelRepository: GroupDiscordChannelRepository,
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val discordApiAdapter: DiscordApiAdapter,
    @param:Value("\${discord.guild-id:}") private val discordGuildId: String,
) {
    private val logger = LoggerFactory.getLogger(GroupInfoService::class.java)

    // 그룹원·리스트·카드·기록 수는 각 변경 API가 같은 트랜잭션에서 갱신하는 그룹의 저장값을 반환한다.
    @Transactional(readOnly = true)
    fun getGroupInfoById(
        groupId: Long,
        requesterId: Long,
    ): GroupInfoResult {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        val isMember = groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)
        val requesterRole =
            when {
                group.isLeader(requesterId) -> GroupRequesterRole.LEADER
                isMember -> GroupRequesterRole.MEMBER
                else -> GroupRequesterRole.NONE
            }
        val chatChannel = groupDiscordChannelRepository.findByGroupId(groupId)
        val portfolioCount = groupPortfolioRepository.countPortfolioCardsAndListsByGroupId(groupId)
        val reportCount = groupActivityReportRepository.countPresentationsAndReportsByGroupId(groupId)

        return GroupInfoResult(
            id = group.id,
            title = group.title,
            description = group.description,
            category = group.category,
            status = group.status,
            requesterRole = requesterRole,
            isBookmarked = groupBookmarkRepository.existsByMemberIdAndGroupId(requesterId, groupId),
            isInChatRoom = if (chatChannel == null || !isMember) false else isRequesterInChannel(chatChannel.discordChannelId, requesterId),
            interests = group.interests,
            // 빈 항목은 빼고, 남는 항목이 없으면 null로 반환한다.
            techStack = group.techStackNames?.filter { it.isNotEmpty() }?.ifEmpty { null },
            repositoryUrls = group.repositoryUrlList?.filter { it.isNotEmpty() }?.ifEmpty { null },
            memberCount = group.memberCount,
            listCount = group.listCount,
            cardCount = group.cardCount,
            recordCount = group.recordCount,
            createdAt = group.createdAt,
            lastActivityAt = group.lastActivityAt,
            allowJoin = group.allowJoin,
            visibility = group.visibility,
            isPortfolioPublished = group.isPortfolioPublished,
            portfolioListCount = portfolioCount.listCount,
            portfolioCardCount = portfolioCount.cardCount,
            seminarCount = reportCount.presentationCount,
            reportCount = reportCount.reportCount,
            hasChatRoom = chatChannel != null,
            chatRoomUrl = chatChannel?.let { "https://discord.com/channels/$discordGuildId/${it.discordChannelId}" },
        )
    }

    // Discord 연동이 없으면 채널에 있을 수 없으므로 false다. Discord 조회에 실패해도 그룹 정보 조회는 성공시킨다.
    private fun isRequesterInChannel(
        discordChannelId: String,
        requesterId: Long,
    ): Boolean? {
        val discordIntegration = discordIntegrationRepository.findByUserId(requesterId) ?: return false
        return try {
            discordApiAdapter.isUserInChannel(discordChannelId, discordIntegration.discordUserId)
        } catch (e: Exception) {
            logger.warn("그룹 채팅방 참여 여부를 확인하지 못했습니다. channelId=$discordChannelId, requesterId=$requesterId", e)
            null
        }
    }
}
