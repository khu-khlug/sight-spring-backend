package com.sight.controllers.http.dto

import java.time.Instant

data class GetBookResponse(
    val bookId: String,
    val title: String,
    val coverImageUrl: String,
    val author: String,
    val publisher: String,
    val publishedYear: Int,
    val totalCount: Int,
    val availableCount: Int,
    val isbn: String,
    val description: String,
    val itemList: List<GetBookItemResponse>,
)

data class GetBookItemResponse(
    val itemId: String,
    val registeredAt: Instant,
    val borrowerInfo: GetBookBorrowerInfoResponse?,
)

data class GetBookBorrowerInfoResponse(
    val borrowerUserId: Long,
    val borrowerUserName: String,
    val borrowedAt: Instant,
)
