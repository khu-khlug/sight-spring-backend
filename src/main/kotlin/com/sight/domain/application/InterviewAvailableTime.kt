package com.sight.domain.application

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "interview_available_time")
data class InterviewAvailableTime(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "application_form_id", nullable = false, length = 100)
    val applicationFormId: String,

    // Format: yyyy-MM-dd HH:mm; timezone: KST
    @Column(name = "available_at", nullable = false, length = 50)
    val availableAt: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
