package com.sight.service

import com.sight.domain.discord.DiscordRoleType
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.MemberRepository
import com.sight.service.discord.DiscordApiAdapter
import com.sight.service.discord.DiscordApiModifyMemberParams
import org.springframework.stereotype.Service

@Service
class DiscordMemberService(
    private val discordApiAdapter: DiscordApiAdapter,
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val memberRepository: MemberRepository,
) {
    fun clearDiscordIntegration(userId: Long) {
        val discordIntegration = discordIntegrationRepository.findByUserId(userId) ?: return

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
        val user = memberRepository.findById(userId).orElse(null) ?: return
        val discordIntegration = discordIntegrationRepository.findByUserId(userId) ?: return

        val discordUserId = discordIntegration.discordUserId
        val hasMember = discordApiAdapter.hasMember(discordUserId)
        if (!hasMember) {
            return
        }

        discordApiAdapter.modifyMember(
            DiscordApiModifyMemberParams(
                discordUserId = discordUserId,
                nickname = user.realname,
                roles = calcRoles(user),
            ),
        )
    }

    private fun calcRoles(member: Member): List<DiscordRoleType> {
        val roles = mutableListOf<DiscordRoleType>()

        if (member.status == UserStatus.ACTIVE && member.studentStatus != StudentStatus.GRADUATE) {
            roles.add(DiscordRoleType.MEMBER)
        }

        if (member.status == UserStatus.ACTIVE && member.studentStatus == StudentStatus.GRADUATE) {
            roles.add(DiscordRoleType.GRADUATED_MEMBER)
        }

        if (member.manager) {
            roles.add(DiscordRoleType.MANAGER)
        }

        return roles
    }
}
