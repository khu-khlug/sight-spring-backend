package com.sight.domain.groupmatching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "group_matching_answer")
data class GroupMatchingAnswer(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false, length = 255)
    val groupType: GroupMatchingType,

    @Column(name = "is_prefer_online", nullable = false, columnDefinition = "TINYINT")
    val isPreferOnline: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_frequency", nullable = false, length = 255)
    val activityFrequency: ActivityFrequency,

    @Column(name = "activity_format", nullable = false, length = 500)
    val activityFormat: String,

    @Column(name = "other_suggestions", nullable = true, length = 1000)
    val otherSuggestions: String? = null,

    @Column(name = "custom_option", nullable = true, length = 255)
    val customOption: String? = null,

    @Column(name = "role", nullable = true, length = 100)
    val role: String? = null,

    @Column(name = "has_idea", nullable = true, columnDefinition = "TINYINT")
    val hasIdea: Boolean? = null,

    @Column(name = "idea", nullable = true, length = 1000)
    val idea: String? = null,

    @Column(name = "group_matching_id", nullable = false, length = 100)
    val groupMatchingId: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
