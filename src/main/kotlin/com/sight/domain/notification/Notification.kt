package com.sight.domain.notification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification",
    indexes = [
        Index(name = "idx_notification_userId_createdAt", columnList = "user_id, created_at DESC"),
    ],
)
data class Notification(
    @Id
    val id: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    val category: NotificationCategory,

    @Column(name = "title", length = 255, nullable = false)
    val title: String,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    val content: String,

    @Column(name = "url")
    val url: String? = null,

    @Column(name = "read_at")
    val readAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
