package com.sight.domain.groupmatching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "group_matching")
data class GroupMatching(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "year", nullable = false)
    val year: Int,

    @Column(name = "semester", nullable = false)
    val semester: Int,

    @Column(name = "closed_at", nullable = false)
    val closedAt: LocalDateTime,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
