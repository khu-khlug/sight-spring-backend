package com.sight.domain.finance

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "transaction",
    indexes = [
        Index(name = "idx_transaction_usedAt_createdAt", columnList = "used_at, created_at"),
    ],
)
data class Transaction(
    @Id
    val id: String,

    @Column(name = "author", nullable = false)
    val author: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: TransactionType,

    @Column(name = "item", length = 255, nullable = false)
    val item: String,

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
