package com.sight.controllers.http.dto

import java.time.Instant

data class GetCurrentBorrowingsResponse(
    val records: List<CurrentBorrowingRecordResponse>,
)

data class CurrentBorrowingRecordResponse(
    val recordId: String,
    val itemId: String,
    val bookId: String,
    val title: String,
    val borrowerUserId: Long,
    val borrowerUserName: String,
    val borrowedAt: Instant,
)
