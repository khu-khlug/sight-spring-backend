package com.sight.service

import com.sight.domain.discord.DiscordRole
import com.sight.repository.DiscordRoleRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
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
                ResponseStatusException(HttpStatus.NOT_FOUND, "디스코드 역할 정보를 찾을 수 없습니다")
            }

        val updatedRole =
            discordRole.copy(
                roleId = roleId,
                updatedAt = LocalDateTime.now(),
            )

        return discordRoleRepository.save(updatedRole)
    }
}
