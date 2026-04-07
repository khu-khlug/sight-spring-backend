package com.sight.service.dto

import java.time.Instant

data class GetBookResult(
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
    val itemList: List<GetBookItemResult>,
)

data class GetBookItemResult(
    val itemId: String,
    val registeredAt: Instant,
    val borrowerInfo: GetBookBorrowerInfoResult?,
)

data class GetBookBorrowerInfoResult(
    val borrowerUserId: Long,
    val borrowerUserName: String,
    val borrowedAt: Instant,
)
