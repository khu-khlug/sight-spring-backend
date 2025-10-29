package com.sight.repository

import com.sight.domain.finance.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByYear(
        year: Int,
        pageable: Pageable,
    ): Page<Transaction>
}
