package com.sight.service

import com.sight.domain.discord.DiscordRole
import com.sight.repository.DiscordRoleRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DiscordRoleService(
    private val discordRoleRepository: DiscordRoleRepository,
) {
    fun getAllDiscordRoles(): List<DiscordRole> {
        return discordRoleRepository.findAll()
    }

    fun updateDiscordRole(
        id: Long,
        roleId: String,
    ): DiscordRole {
        val discordRole =
            discordRoleRepository.findById(id).orElseThrow {
                IllegalArgumentException("Discord role not found with id: $id")
            }

        val updatedRole =
            discordRole.copy(
                roleId = roleId,
                updatedAt = LocalDateTime.now(),
            )

        return discordRoleRepository.save(updatedRole)
    }
}
