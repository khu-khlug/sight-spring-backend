package com.sight.discord.service

import com.sight.discord.adapter.DiscordApiAdapter
import com.sight.discord.adapter.DiscordApiModifyMemberParams
import com.sight.discord.model.DiscordRole
import com.sight.domain.Member
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DiscordMemberService(
    private val discordApiAdapter: DiscordApiAdapter,
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val memberRepository: MemberRepository,
) {
    private val logger = LoggerFactory.getLogger(DiscordMemberService::class.java)

    fun clearDiscordIntegration(userId: Long) {
        val discordIntegration = discordIntegrationRepository.findByUserId(userId)

        if (discordIntegration == null) {
            return
        }

        val discordUserId = discordIntegration.discordUserId
        val hasMember = discordApiAdapter.hasMember(discordUserId)
        if (hasMember) {
            discordApiAdapter.modifyMember(
                DiscordApiModifyMemberParams(
                    discordUserId = discordUserId,
                    roles = emptyList(),
                ),
            )
        }

        discordIntegrationRepository.delete(discordIntegration)
    }

    fun reflectUserInfoToDiscordUser(userId: Long) {
        val user = memberRepository.findById(userId).orElse(null)
        if (user == null) {
            logger.warn("User not found: $userId")
            return
        }

        val discordIntegration = discordIntegrationRepository.findByUserId(userId)
        if (discordIntegration == null) {
            logger.warn("Discord integration not found for user: $userId")
            return
        }

        val discordUserId = discordIntegration.discordUserId
        val hasMember = discordApiAdapter.hasMember(discordUserId)
        if (!hasMember) {
            logger.warn("Discord member not found: $discordUserId")
            return
        }

        logger.info("Reflecting user info to Discord for user: $userId, Discord user: $discordUserId")
        discordApiAdapter.modifyMember(
            DiscordApiModifyMemberParams(
                discordUserId = discordUserId,
                nickname = user.realname,
                roles = calcRoles(user),
            ),
        )
    }

    private fun calcRoles(member: Member): List<DiscordRole> {
        val roles = mutableListOf<DiscordRole>()

        if (member.active && member.state != 2L) {
            roles.add(DiscordRole.MEMBER)
        }

        if (member.active && member.state == 2L) {
            roles.add(DiscordRole.GRADUATED_MEMBER)
        }

        if (member.manager) {
            roles.add(DiscordRole.MANAGER)
        }

        return roles
    }
}
