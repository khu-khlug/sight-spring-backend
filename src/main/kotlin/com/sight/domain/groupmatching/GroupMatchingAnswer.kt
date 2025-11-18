package com.sight.domain.groupmatching

import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupCategoryConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
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

    @Column(name = "group_type", nullable = false, length = 255)
    @Convert(converter = GroupCategoryConverter::class)
    val groupType: GroupCategory,

    @Column(name = "is_prefer_online", nullable = false, columnDefinition = "TINYINT")
    val isPreferOnline: Boolean,

    @Column(name = "group_matching_id", nullable = false, length = 100)
    val groupMatchingId: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
