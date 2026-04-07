package com.sight.service.dto

data class BookStatsResult(
    val totalBookCount: Long,
    val totalItemCount: Long,
    val currentBorrowingCount: Long,
)
