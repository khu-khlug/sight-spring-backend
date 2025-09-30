package com.sight.service.discord

import com.sight.core.exception.InternalServerErrorException
import com.sight.domain.discord.DiscordRoleType
import com.sight.repository.DiscordRoleRepository
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.EnumSet

@Component
class JdaDiscordApiAdapter(
    private val jda: JDA,
    @param:Value("\${discord.guild-id}") private val guildId: String,
    @param:Value("\${discord.categories.group}") private val groupCategoryId: String,

    private val discordRoleRepository: DiscordRoleRepository,
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
                    params.roles.map { role ->
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

    override fun createGroupPrivateTextChannel(channelName: String): TextChannel {
        return try {
            val guild =
                jda.getGuildById(guildId)
                    ?: throw InternalServerErrorException("디스코드 서버를 찾을 수 없습니다. 운영진에게 문의해주세요.")

            val category = guild.getCategoryById(groupCategoryId)
            val channel =
                guild
                    .createTextChannel(channelName, category)
                    .addPermissionOverride(guild.publicRole, null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .complete()

            channel
        } catch (e: Exception) {
            logger.error("Failed to create Discord text channel: $channelName", e)
            throw InternalServerErrorException("디스코드 채널 생성에 실패했습니다. 운영진에게 문의해주세요.")
        }
    }

    override fun addMemberToChannel(
        channelId: String,
        discordUserId: String,
    ) {
        try {
            val guild =
                jda.getGuildById(guildId)
                    ?: throw InternalServerErrorException("디스코드 서버를 찾을 수 없습니다. 운영진에게 문의해주세요.")

            val channel =
                guild.getTextChannelById(channelId)
                    ?: throw InternalServerErrorException("디스코드 채널을 찾을 수 없습니다. 운영진에게 문의해주세요.")

            val member = guild.retrieveMemberById(discordUserId).complete()

            channel.upsertPermissionOverride(member)
                .setAllowed(EnumSet.of(Permission.VIEW_CHANNEL))
                .complete()
        } catch (e: Exception) {
            logger.error("Failed to add member $discordUserId to channel $channelId", e)
            throw InternalServerErrorException("채널에 멤버를 추가하는데 실패했습니다. 운영진에게 문의해주세요.")
        }
    }

    override fun isUserInChannel(
        channelId: String,
        discordUserId: String,
    ): Boolean {
        return try {
            val guild = jda.getGuildById(guildId) ?: return false
            val channel = guild.getTextChannelById(channelId) ?: return false
            val member = guild.retrieveMemberById(discordUserId).complete()

            val permissionOverride = channel.getPermissionOverride(member)
            permissionOverride?.allowed?.contains(Permission.VIEW_CHANNEL) == true
        } catch (e: Exception) {
            logger.debug("Failed to check if user $discordUserId is in channel $channelId", e)
            false
        }
    }

    private fun getRoleByDiscordRole(
        guild: Guild,
        type: DiscordRoleType,
    ): Role {
        val discordRole =
            discordRoleRepository.findByRoleType(type)
                ?: throw InternalServerErrorException("적절한 디스코드 역할을 찾을 수 없습니다. 운영진에게 문의해주세요.")

        return guild.getRoleById(discordRole.roleId)
            ?: throw InternalServerErrorException("잘못된 디스코드 역할 아이디입니다. 운영진에게 문의해주세요.")
    }
}
