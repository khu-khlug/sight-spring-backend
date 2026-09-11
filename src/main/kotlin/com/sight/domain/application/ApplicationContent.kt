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
class ApplicationContent(
    id: String,
    applicationFormId: String,
    questionId: String,
    content: String,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String = id

    @Column(name = "application_form_id", nullable = false, length = 100)
    val applicationFormId: String = applicationFormId

    @Column(name = "question_id", nullable = false, length = 100)
    val questionId: String = questionId

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String = content
        private set

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = createdAt

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = updatedAt

    fun updateContent(content: String) {
        this.content = content
    }
}
