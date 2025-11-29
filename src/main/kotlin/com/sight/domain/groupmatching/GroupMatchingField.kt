package com.sight.domain.groupmatching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "group_matching_field")
data class GroupMatchingField(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "name", nullable = false, length = 255)
    val name: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "obsoleted_at", nullable = true)
    var obsoletedAt: LocalDateTime? = null,
)
