package com.sight.domain.application

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "application_question")
data class ApplicationQuestion(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,

    @Column(name = "title", nullable = false, length = 255)
    val title: String,

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "min_length", nullable = false)
    val minLength: Int,

    @Column(name = "`order`", nullable = true)
    val order: Int? = null,

    @Column(name = "is_exposed", nullable = false, columnDefinition = "TINYINT")
    val isExposed: Boolean,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
