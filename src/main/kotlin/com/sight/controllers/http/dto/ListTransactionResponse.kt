package com.sight.controllers.http.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class ListTransactionResponse(
    val id: String,
    val author: Long,
    val item: String?,
    val price: Long,
    val quantity: Long,
    val total: Long,
    val cumulative: Long,
    val place: String?,
    val note: String?,
    val usedAt: LocalDate,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class ListTransactionsResponse(
    val count: Long,
    val transactions: List<ListTransactionResponse>,
)
