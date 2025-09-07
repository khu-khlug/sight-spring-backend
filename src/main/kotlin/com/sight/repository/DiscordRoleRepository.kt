package com.sight.repository

import com.sight.domain.discord.DiscordRole
import com.sight.domain.discord.DiscordRoleType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DiscordRoleRepository : JpaRepository<DiscordRole, Long> {
    fun findByRoleType(roleType: DiscordRoleType): DiscordRole?
}
