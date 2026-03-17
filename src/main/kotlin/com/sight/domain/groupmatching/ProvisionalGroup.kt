package com.sight.domain.groupmatching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(name = "provisional_group")
data class ProvisionalGroup(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    @Column(name = "group_matching_id", nullable = false, length = 100)
    val groupMatchingId: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
