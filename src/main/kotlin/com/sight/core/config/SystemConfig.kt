package com.sight.core.config

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "system_config")
data class SystemConfig(
    @Id
    @Column(name = "id", nullable = false, length = 26)
    val id: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "config_key", nullable = false, unique = true, length = 255)
    val configKey: ConfigKey,

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    val configValue: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
