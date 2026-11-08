package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "group_discord_channel")
data class GroupDiscordChannel(
    @Id
    @Column(name = "id", nullable = false, length = 32)
    val id: String,

    @Column(name = "group_id", nullable = false)
    val groupId: Long,

    @Column(name = "discord_channel_id", nullable = false, length = 255)
    val discordChannelId: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
