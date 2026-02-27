package com.sight.repository

import com.sight.domain.finance.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.time.LocalDateTime

interface TransactionRepository : JpaRepository<Transaction, String> {
    @Query(
        """
        SELECT t FROM Transaction t
        WHERE t.usedAt >= :startDate AND t.usedAt < :endDate
        ORDER BY t.usedAt DESC, t.createdAt DESC
        """,
    )
    fun findByUsedAtBetween(
        startDate: LocalDate,
        endDate: LocalDate,
        pageable: Pageable,
    ): Page<Transaction>

    @Query(
        "SELECT t FROM Transaction t ORDER BY t.usedAt DESC, t.createdAt DESC LIMIT 1",
    )
    fun findLatest(): Transaction?

    @Query(
        """
        SELECT t FROM Transaction t
        WHERE t.usedAt <= :usedAt
        ORDER BY t.usedAt DESC, t.createdAt DESC
        LIMIT 1
        """,
    )
    fun findLatestOnOrBefore(usedAt: LocalDate): Transaction?

    @Query(
        """
        SELECT t FROM Transaction t
        WHERE t.usedAt > :usedAt
        ORDER BY t.usedAt ASC, t.createdAt ASC
        """,
    )
    fun findAfterDate(usedAt: LocalDate): List<Transaction>

    @Query(
        """
        SELECT t FROM Transaction t
        WHERE t.usedAt < :usedAt
           OR (t.usedAt = :usedAt AND t.createdAt < :createdAt)
        ORDER BY t.usedAt DESC, t.createdAt DESC
        LIMIT 1
        """,
    )
    fun findPredecessor(
        usedAt: LocalDate,
        createdAt: LocalDateTime,
    ): Transaction?

    @Query(
        """
        SELECT t FROM Transaction t
        WHERE t.usedAt > :usedAt
           OR (t.usedAt = :usedAt AND t.createdAt > :createdAt)
        ORDER BY t.usedAt ASC, t.createdAt ASC
        """,
    )
    fun findAfter(
        usedAt: LocalDate,
        createdAt: LocalDateTime,
    ): List<Transaction>
}
