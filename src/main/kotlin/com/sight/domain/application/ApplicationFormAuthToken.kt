package com.sight.domain.application

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "application_form_auth_token")
data class ApplicationFormAuthToken(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "application_form_id", nullable = false, length = 100)
    val applicationFormId: String,

    @Column(name = "token", nullable = false, length = 255)
    val token: String,

    @Column(name = "expired_at", nullable = false)
    val expiredAt: LocalDateTime,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
