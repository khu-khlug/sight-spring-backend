package com.sight.controllers.http

import com.sight.controllers.http.dto.ListTransactionResponse
import com.sight.controllers.http.dto.ListTransactionsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.TransactionService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class TransactionController(
    private val transactionService: TransactionService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/transactions")
    fun getTransactions(
        @RequestParam year: Int,
        @RequestParam(defaultValue = "0") @Min(0, message = "offset은 0 이상이어야 합니다") offset: Int,
        @RequestParam(defaultValue = "20") @Min(1, message = "limit은 최소 1입니다") @Max(50, message = "limit은 최대 50입니다") limit: Int,
    ): ListTransactionsResponse {
        val result = transactionService.listTransactions(year, offset, limit)
        val transactionResponses =
            result.transactions.map { transaction ->
                ListTransactionResponse(
                    id = transaction.id,
                    author = transaction.author,
                    year = transaction.year,
                    month = transaction.month,
                    item = transaction.item,
                    price = transaction.price,
                    quantity = transaction.quantity,
                    total = transaction.total,
                    cumulative = transaction.cumulative,
                    place = transaction.place,
                    note = transaction.note,
                    usedAt = transaction.usedAt,
                    createdAt = transaction.createdAt,
                    updatedAt = transaction.updatedAt,
                )
            }
        return ListTransactionsResponse(
            count = result.count,
            transactions = transactionResponses,
        )
    }

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/transactions/{id}")
    fun deleteTransaction(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        transactionService.deleteTransaction(id)
        return ResponseEntity.noContent().build()
    }
}
