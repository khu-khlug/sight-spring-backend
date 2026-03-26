package com.sight.service

import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import com.sight.repository.BookBorrowRecordRepository
import com.sight.service.dto.BookStatsResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
    private val bookInfoRepository: BookInfoRepository,
    private val bookItemRepository: BookItemRepository,
    private val bookBorrowRecordRepository: BookBorrowRecordRepository,
) {
    @Transactional(readOnly = true)
    fun getStats(): BookStatsResult {
        val totalBookCount = bookInfoRepository.count()
        val totalItemCount = bookItemRepository.count()
        val currentBorrowingCount = bookBorrowRecordRepository.countByReturnedAtIsNull()
        return BookStatsResult(
            totalBookCount = totalBookCount,
            totalItemCount = totalItemCount,
            currentBorrowingCount = currentBorrowingCount,
        )
    }
}
