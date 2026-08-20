package com.sight.domain.supportrequest

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "support_request")
class SupportRequest(
    @Id
    @Column(name = "id", nullable = false, length = 100)
    val id: String,
    @Column(name = "requester_id", nullable = false)
    val requesterId: Long,
    category: SupportRequestCategory,
    title: String,
    content: String,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    var category: SupportRequestCategory = category
        private set

    @Column(name = "title", nullable = false, length = 255)
    var title: String = title
        private set

    @Lob
    @Column(name = "content", nullable = false)
    var content: String = content
        private set

    fun update(
        category: SupportRequestCategory,
        title: String,
        content: String,
    ) {
        this.category = category
        this.title = title
        this.content = content
    }
}
