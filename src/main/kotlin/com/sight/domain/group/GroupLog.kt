package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "khlug_group_log")
data class GroupLog(
    @Id
    @Column(name = "id", nullable = false)
    val id: Long,

    @Column(name = "`group`", nullable = false)
    val group: Long,

    @Column(name = "member", nullable = false)
    val member: Long,

    @Column(name = "message", nullable = false, columnDefinition = "LONGTEXT")
    val message: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
