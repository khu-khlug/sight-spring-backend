package com.sight.repository

import com.sight.domain.book.BookInfo
import org.springframework.data.jpa.repository.JpaRepository

interface BookInfoRepository : JpaRepository<BookInfo, String> {
    fun findByIsbn(isbn: String): BookInfo?
}
