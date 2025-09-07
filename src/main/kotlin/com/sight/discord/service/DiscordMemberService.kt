package com.sight.discord.service

import StudentStatus
import UserStatus
import com.sight.discord.adapter.DiscordApiAdapter
import com.sight.discord.adapter.DiscordApiModifyMemberParams
import com.sight.discord.model.DiscordRole
import com.sight.domain.member.Member
import com.sight.repository.DiscordIntegrationRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class DiscordMemberService(
    private val discordApiAdapter: DiscordApiAdapter,
    private val discordIntegrationRepository: DiscordIntegrationRepository,
    private val memberRepository: MemberRepository,
) {
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

    private fun calcRoles(member: Member): List<DiscordRole> {
        val roles = mutableListOf<DiscordRole>()

        if (member.status == UserStatus.ACTIVE && member.studentStatus != StudentStatus.GRADUATE) {
            roles.add(DiscordRole.MEMBER)
        }

        if (member.status == UserStatus.ACTIVE && member.studentStatus == StudentStatus.GRADUATE) {
            roles.add(DiscordRole.GRADUATED_MEMBER)
        }

        if (member.manager) {
            roles.add(DiscordRole.MANAGER)
        }

        return roles
    }
}
