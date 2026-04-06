package com.sight.repository

import com.sight.domain.book.BookItem
import org.springframework.data.jpa.repository.JpaRepository

interface BookItemRepository : JpaRepository<BookItem, String> {
    fun findAllByBookInfoId(bookInfoId: String): List<BookItem>
}
