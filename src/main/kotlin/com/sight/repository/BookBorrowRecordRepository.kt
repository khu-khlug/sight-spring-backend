package com.sight.repository

import com.sight.domain.book.BookBorrowRecord
import org.springframework.data.jpa.repository.JpaRepository

interface BookBorrowRecordRepository : JpaRepository<BookBorrowRecord, String> {
    fun findAllByItemId(itemId: String): List<BookBorrowRecord>

    fun findAllByUserId(userId: Long): List<BookBorrowRecord>

<<<<<<< HEAD
    fun findAllByUserIdAndReturnedAtIsNull(userId: Long): List<BookBorrowRecord>

=======
>>>>>>> 76b9935 (feat: 현재대출기록열람 borrowedAt 내림차순 정렬 추가)
    fun findAllByReturnedAtIsNullOrderByBorrowedAtDesc(): List<BookBorrowRecord>
}
