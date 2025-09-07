package com.sight.service.discord

import com.sight.domain.discord.DiscordRole
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Role
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JdaDiscordApiAdapter(
    private val jda: JDA,
    @Value("\${discord.guild-id}") private val guildId: String,
) : DiscordApiAdapter {
    private val logger = LoggerFactory.getLogger(JdaDiscordApiAdapter::class.java)

    override fun hasMember(discordUserId: String): Boolean {
        return try {
            val guild = jda.getGuildById(guildId) ?: return false
            guild.retrieveMemberById(discordUserId).complete()
            true
        } catch (e: Exception) {
            logger.debug("Member not found in Discord: $discordUserId")
            false
        }
    }

    override fun modifyMember(params: DiscordApiModifyMemberParams) {
        try {
            val guild = jda.getGuildById(guildId) ?: return
            val member = guild.retrieveMemberById(params.discordUserId).complete()

            if (params.nickname != null) {
                guild.modifyNickname(member, params.nickname).queue(
                    { logger.info("Updated Discord nickname for ${params.discordUserId}: ${params.nickname}") },
                    { error -> logger.error("Failed to update nickname for ${params.discordUserId}", error) },
                )
            }

            if (params.roles != null) {
                val discordRoles =
                    params.roles.mapNotNull { role ->
                        getRoleByDiscordRole(guild, role)
                    }
                guild.modifyMemberRoles(member, discordRoles).queue(
                    { logger.info("Updated Discord roles for ${params.discordUserId}: ${params.roles}") },
                    { error -> logger.error("Failed to update roles for ${params.discordUserId}", error) },
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to modify Discord member ${params.discordUserId}", e)
        }
    }

    private fun getRoleByDiscordRole(
        guild: net.dv8tion.jda.api.entities.Guild,
        discordRole: DiscordRole,
    ): Role? {
        val roleName =
            when (discordRole) {
                DiscordRole.MEMBER -> "Member"
                DiscordRole.GRADUATED_MEMBER -> "Graduated Member"
                DiscordRole.MANAGER -> "Manager"
            }
        return guild.getRolesByName(roleName, true).firstOrNull()
    }
}
