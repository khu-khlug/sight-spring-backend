package com.sight.controllers.http.dto

import com.sight.domain.book.BookCategory

data class ListBooksResponse(
    val bookList: List<ListBookResponse>,
)

data class ListBookResponse(
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
