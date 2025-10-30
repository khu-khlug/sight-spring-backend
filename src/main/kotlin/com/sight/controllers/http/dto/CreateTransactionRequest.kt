package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.sight.domain.finance.TransactionType
import jakarta.validation.constraints.Min
import java.time.LocalDate

data class CreateTransactionRequest(
    @field:JsonProperty("type")
    val type: TransactionType,

    @field:JsonProperty("item")
    val item: String?,

    @field:Min(value = 0, message = "가격은 0 이상이어야 합니다")
    @field:JsonProperty("price")
    val price: Long,

    @field:Min(value = 1, message = "수량은 1 이상이어야 합니다")
    @field:JsonProperty("quantity")
    val quantity: Long,

    @field:JsonProperty("place")
    val place: String?,

    @field:JsonProperty("note")
    val note: String?,

    @field:JsonProperty("usedAt")
    val usedAt: LocalDate,
)
