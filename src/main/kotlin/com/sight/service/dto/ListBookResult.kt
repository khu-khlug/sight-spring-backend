package com.sight.service.dto

import com.sight.domain.book.BookCategory

data class ListBookResult(
    val bookId: String,
    val title: String,
    val coverImageUrl: String,
    val author: String,
    val publisher: String,
    val publishedYear: Int,
    val totalCount: Int,
    val availableCount: Int,
    val category: BookCategory,
)
