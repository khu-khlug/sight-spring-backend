package com.sight.repository

import com.sight.domain.finance.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

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
        "SELECT t FROM Transaction t ORDER BY t.usedAt DESC, t.createdAt DESC",
    )
    fun findLatest(): Transaction?
}
