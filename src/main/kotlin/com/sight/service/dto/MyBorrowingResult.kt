package com.sight.service.dto

import java.time.Instant

data class MyBorrowingResult(
    val bookId: String,
    val itemId: String,
    val title: String,
    val borrowedAt: Instant,
)
