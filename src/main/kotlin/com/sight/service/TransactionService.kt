package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.controllers.http.dto.CreateTransactionRequest
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.finance.Transaction
import com.sight.domain.finance.TransactionType
import com.sight.repository.TransactionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

data class TransactionListResult(
    val count: Long,
    val transactions: List<Transaction>,
)

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
) {
    @Transactional(readOnly = true)
    fun getCurrentCumulative(): Long {
        val latestTransaction =
            transactionRepository.findLatest()
                ?: throw NotFoundException("No transaction exists")

        return latestTransaction.cumulative
    }

    @Transactional(readOnly = true)
    fun listTransactions(
        year: Int,
        offset: Int,
        limit: Int,
    ): TransactionListResult {
        val pageNumber = offset / limit
        val pageable = PageRequest.of(pageNumber, limit)
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year + 1, 1, 1)
        val page: Page<Transaction> =
            transactionRepository.findByUsedAtBetween(startDate, endDate, pageable)

        return TransactionListResult(
            count = page.totalElements,
            transactions = page.content,
        )
    }

    @Transactional
    fun deleteTransaction(id: String) {
        val transaction =
            transactionRepository.findById(id).orElseThrow {
                NotFoundException("해당 장부 내역을 찾을 수 없습니다")
            }

        transactionRepository.delete(transaction)
    }

    @Transactional
    fun createTransaction(
        request: CreateTransactionRequest,
        authorId: Long,
    ): Transaction {
        val today = LocalDate.now()
        if (request.usedAt.isAfter(today)) {
            throw UnprocessableEntityException("미래 날짜는 입력할 수 없습니다")
        }

        val total = request.price * request.quantity

        val prevCumulative = transactionRepository.findLatest()?.cumulative ?: 0L
        val cumulative =
            when (request.type) {
                TransactionType.INCOME -> prevCumulative + total
                TransactionType.EXPENSE -> prevCumulative - total
            }

        val transaction =
            Transaction(
                id = UlidCreator.getUlid().toString(),
                author = authorId,
                type = request.type,
                item = request.item,
                price = request.price,
                quantity = request.quantity,
                total = total,
                cumulative = cumulative,
                place = request.place,
                note = request.note,
                usedAt = request.usedAt,
            )

        return transactionRepository.save(transaction)
    }
}
