package com.sight.domain.groupmatching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "matched_group")
data class MatchedGroup(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "group_id", nullable = false)
    val groupId: Long,

    @Column(name = "answer_id", nullable = false, length = 100)
    val answerId: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
