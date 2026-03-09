package com.sight.domain.groupmatching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "group_matching_option")
data class GroupMatchingOption(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "group_matching_id", nullable = false, length = 100)
    val groupMatchingId: String,

    @Column(name = "name", nullable = false, length = 255)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "group_matching_type", nullable = false, length = 255)
    val groupMatchingType: GroupMatchingType,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
