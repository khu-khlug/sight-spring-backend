package com.sight.controllers.http.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class CreateTransactionResponse(
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
)
