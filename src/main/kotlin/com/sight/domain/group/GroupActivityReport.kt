package com.sight.domain.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "group_activity_report")
data class GroupActivityReport(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "group_id", nullable = false)
    val groupId: Long,

    @Column(name = "seminar_id", nullable = false, length = 100)
    val seminarId: String,

    @Column(name = "report_file_r2_key", nullable = false, length = 255)
    val reportFileR2Key: String,

    @Column(name = "is_presentation", nullable = false, columnDefinition = "TINYINT")
    val isPresentation: Boolean,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
