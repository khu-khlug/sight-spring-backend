package com.sight.controllers.http.dto

import java.time.Instant

data class GetMyBorrowingsResponse(
    val currentBorrowings: List<MyBorrowingItemResponse>,
)

data class MyBorrowingItemResponse(
    val bookId: String,
    val itemId: String,
    val title: String,
    val borrowedAt: Instant,
)
