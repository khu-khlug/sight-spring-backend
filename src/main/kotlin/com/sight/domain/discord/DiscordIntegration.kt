package com.sight.domain.discord

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "discord_integration",
    indexes = [
        Index(name = "idx_discord_int_user_id", columnList = "user_id"),
        Index(name = "idx_discord_int_discord_user_id", columnList = "discord_user_id"),
    ],
)
data class DiscordIntegration(
    @Id
    @Column(name = "id", length = 32)
    val id: String,

    @Column(name = "user_id", nullable = false)
    val userId: Int,

    @Column(name = "discord_user_id", length = 32, nullable = false)
    val discordUserId: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,
)
