package com.sight.controllers.http.dto

data class GetBookStatsResponse(
    val totalBookCount: Long,
    val totalItemCount: Long,
    val currentBorrowingCount: Long,
)
