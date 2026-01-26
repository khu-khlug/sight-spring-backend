package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.io.Serializable
import java.time.LocalDateTime

data class GroupBookmarkId(
    val member: Long = 0,
    val group: Long = 0,
) : Serializable

@Entity
@Table(name = "khlug_group_bookmark")
@IdClass(GroupBookmarkId::class)
data class GroupBookmark(
    @Id
    @Column(name = "member", nullable = false)
    val member: Long,

    @Id
    @Column(name = "`group`", nullable = false)
    val group: Long,

    @Column(name = "group_order", nullable = false)
    val groupOrder: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
