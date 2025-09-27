package com.sight.service.discord

import com.sight.core.exception.InternalServerErrorException
import com.sight.domain.discord.DiscordRoleType
import com.sight.repository.DiscordRoleRepository
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JdaDiscordApiAdapter(
    private val jda: JDA,
    @field:Value("\${discord.guild-id}") private val guildId: String,
    @field:Value("\${discord.categories.group") private val groupCategoryId: String,

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

    override fun createGroupTextChannel(channelName: String): String {
        return try {
            val guild =
                jda.getGuildById(guildId)
                    ?: throw InternalServerErrorException("디스코드 서버를 찾을 수 없습니다. 운영진에게 문의해주세요.")

            val category = guild.getCategoryById(groupCategoryId)
            if (category == null) {
                logger.warn("그룹 디스코드 채널 생성에 사용될 카테고리가 지정되지 않았습니다.")
            }

            val channel = guild.createTextChannel(channelName, category).complete()
            channel.id
        } catch (e: Exception) {
            logger.error("Failed to create Discord text channel: $channelName", e)
            throw InternalServerErrorException("디스코드 채널 생성에 실패했습니다. 운영진에게 문의해주세요.")
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
