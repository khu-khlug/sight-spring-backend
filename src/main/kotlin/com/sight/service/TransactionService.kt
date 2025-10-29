package com.sight.service

import com.sight.domain.finance.Transaction
import com.sight.repository.TransactionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class TransactionListResult(
    val count: Long,
    val transactions: List<Transaction>,
)

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
) {
    @Transactional(readOnly = true)
    fun listTransactions(
        year: Int,
        offset: Int,
        limit: Int,
    ): TransactionListResult {
        val pageNumber = offset / limit

        val sort =
            Sort.by(
                Sort.Order.desc("usedAt"),
                Sort.Order.desc("createdAt"),
            )

        val pageable = PageRequest.of(pageNumber, limit, sort)
        val page: Page<Transaction> = transactionRepository.findByYear(year, pageable)

        return TransactionListResult(
            count = page.totalElements,
            transactions = page.content,
        )
    }
}
