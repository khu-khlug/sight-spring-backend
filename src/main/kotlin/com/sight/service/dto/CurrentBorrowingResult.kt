package com.sight.service.dto

import java.time.Instant

data class CurrentBorrowingResult(
    val recordId: String,
    val itemId: String,
    val bookId: String,
    val title: String,
    val borrowerUserId: Long,
    val borrowerUserName: String,
    val borrowedAt: Instant,
)
