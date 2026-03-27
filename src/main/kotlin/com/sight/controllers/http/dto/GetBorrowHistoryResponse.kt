package com.sight.controllers.http.dto

import java.time.Instant

data class GetBorrowHistoryResponse(
    val records: List<BorrowHistoryRecordResponse>,
)

data class BorrowHistoryRecordResponse(
    val recordId: String,
    val itemId: String,
    val bookId: String,
    val title: String,
    val borrowerUserId: Long,
    val borrowerUserName: String,
    val borrowedAt: Instant,
    val returnedAt: Instant?,
)
