package com.sight.repository

import com.sight.domain.book.BookBorrowRecord
import org.springframework.data.jpa.repository.JpaRepository

interface BookBorrowRecordRepository : JpaRepository<BookBorrowRecord, String> {
    fun findAllByItemId(itemId: String): List<BookBorrowRecord>

    fun findAllByUserId(userId: Long): List<BookBorrowRecord>
}
