package com.sight.service

import com.sight.repository.BookBorrowRecordRepository
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import com.sight.service.dto.BookStatsResult
import com.sight.service.dto.ListBookResult
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

    @Transactional(readOnly = true)
    fun listBooks(): List<ListBookResult> {
        val bookInfos = bookInfoRepository.findAll()
        val itemsByBookInfoId = bookItemRepository.findAll().groupBy { it.bookInfoId }
        val borrowedItemIds = bookBorrowRecordRepository.findAllByReturnedAtIsNull().map { it.itemId }.toSet()

        return bookInfos.map { bookInfo ->
            val items = itemsByBookInfoId[bookInfo.id] ?: emptyList()
            val totalCount = items.size
            val availableCount = items.count { it.id !in borrowedItemIds }
            ListBookResult(
                bookId = bookInfo.id,
                title = bookInfo.title,
                coverImageUrl = bookInfo.coverImageUrl,
                author = bookInfo.author,
                publisher = bookInfo.publisher,
                publishedYear = bookInfo.publishedYear,
                totalCount = totalCount,
                availableCount = availableCount,
            )
        }
    }
}
