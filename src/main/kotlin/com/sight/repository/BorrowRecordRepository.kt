package com.sight.repository

import com.sight.domain.book.BorrowRecord
import org.springframework.data.jpa.repository.JpaRepository

interface BorrowRecordRepository : JpaRepository<BorrowRecord, String> {
    fun findAllByItemId(itemId: String): List<BorrowRecord>

    fun findAllByUserId(userId: Long): List<BorrowRecord>
}
