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
class ApplicationQuestion(
    id: String,
    title: String,
    description: String,
    minLength: Int,
    order: Int? = null,
    isExposed: Boolean,
    createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String = id

    @Column(name = "title", nullable = false, length = 255)
    var title: String = title
        private set

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = description
        private set

    @Column(name = "min_length", nullable = false)
    var minLength: Int = minLength
        private set

    @Column(name = "`order`", nullable = true)
    var order: Int? = order
        private set

    @Column(name = "is_exposed", nullable = false, columnDefinition = "TINYINT")
    var isExposed: Boolean = isExposed
        private set

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = createdAt

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = updatedAt

    init {
        requireValidState(minLength, order)
    }

    fun update(
        title: String,
        description: String,
        minLength: Int,
        order: Int?,
        isExposed: Boolean,
    ) {
        requireValidState(minLength, order)

        this.title = title
        this.description = description
        this.minLength = minLength
        this.order = order
        this.isExposed = isExposed
    }

    private fun requireValidState(
        minLength: Int,
        order: Int?,
    ) {
        require(minLength >= 0) { "최소 글자 수는 0 이상이어야 합니다" }
        require(order == null || order > 0) { "문항 순서는 1 이상이어야 합니다" }
    }
}
