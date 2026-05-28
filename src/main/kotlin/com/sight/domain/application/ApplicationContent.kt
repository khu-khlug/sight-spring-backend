package com.sight.domain.application

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "application_content")
data class ApplicationContent(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "application_form_id", nullable = false, length = 100)
    val applicationFormId: String,

    @Column(name = "question_id", nullable = false, length = 100)
    val questionId: String,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
