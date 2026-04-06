package com.sight.domain.book

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(name = "borrow_record")
data class BookBorrowRecord(
    @Id
    @Column(name = "id", nullable = false, length = 26)
    val id: String,

    @Column(name = "item_id", nullable = false, length = 26)
    val itemId: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @CreationTimestamp
    @Column(name = "borrowed_at", nullable = false)
    val borrowedAt: Instant = Instant.now(),

    @Column(name = "returned_at")
    val returnedAt: Instant? = null,
)
