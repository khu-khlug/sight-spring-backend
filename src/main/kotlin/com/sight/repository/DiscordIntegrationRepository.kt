package com.sight.repository

import com.sight.domain.discord.DiscordIntegration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DiscordIntegrationRepository : JpaRepository<DiscordIntegration, String> {
    fun findByDiscordUserId(discordUserId: String): DiscordIntegration?

    fun findByUserId(userId: Long): DiscordIntegration?
}
