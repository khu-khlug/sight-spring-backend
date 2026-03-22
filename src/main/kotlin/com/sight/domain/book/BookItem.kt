package com.sight.domain.book

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(name = "book_item")
data class BookItem(
    @Id
    @Column(name = "id", nullable = false, length = 26)
    val id: String,

    @Column(name = "book_info_id", nullable = false, length = 26)
    val bookInfoId: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
