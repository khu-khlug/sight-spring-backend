package com.sight.domain.application

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
@Table(name = "application_form")
data class ApplicationForm(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "info21_id", nullable = false, length = 100)
    val info21Id: String,

    @Column(name = "submittee", nullable = false, length = 255)
    val submittee: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    val status: ApplicationFormStatus,

    @Column(name = "assigned_user_id", nullable = true)
    val assignedUserId: Long? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
