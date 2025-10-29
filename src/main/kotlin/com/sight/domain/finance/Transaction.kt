package com.sight.domain.finance

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "khlug_finance")
data class Transaction(
    @Id
    val id: Long,

    @Column(name = "author", nullable = false)
    val author: Long,

    @Column(name = "year", nullable = false)
    val year: Int,

    @Column(name = "month", nullable = false)
    val month: Int,

    @Column(name = "item", length = 255)
    val item: String?,

    @Column(name = "price", nullable = false)
    val price: Long,

    @Column(name = "quantity", nullable = false)
    val quantity: Long,

    @Column(name = "total", nullable = false)
    val total: Long,

    @Column(name = "cumulative", nullable = false)
    val cumulative: Long,

    @Column(name = "place", length = 255)
    val place: String?,

    @Column(name = "note", length = 255)
    val note: String?,

    @Column(name = "used_at", nullable = false)
    val usedAt: LocalDate,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
