package com.sight.repository

import com.sight.domain.book.BookBorrowRecord
import org.springframework.data.jpa.repository.JpaRepository

interface BookBorrowRecordRepository : JpaRepository<BookBorrowRecord, String> {
    fun findAllByItemId(itemId: String): List<BookBorrowRecord>

    fun findAllByUserId(userId: Long): List<BookBorrowRecord>

    fun countByReturnedAtIsNull(): Long

    fun findAllByReturnedAtIsNull(): List<BookBorrowRecord>

    fun findAllByItemIdInAndReturnedAtIsNull(itemIds: List<String>): List<BookBorrowRecord>

    fun findByUserIdAndItemIdInAndReturnedAtIsNull(
        userId: Long,
        itemIds: List<String>,
    ): BookBorrowRecord?

    fun findAllByUserIdAndReturnedAtIsNull(userId: Long): List<BookBorrowRecord>

    fun findAllByReturnedAtIsNullOrderByBorrowedAtDesc(): List<BookBorrowRecord>

    fun findAllByOrderByBorrowedAtDesc(): List<BookBorrowRecord>
}
