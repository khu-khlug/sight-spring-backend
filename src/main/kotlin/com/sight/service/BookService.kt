package com.sight.service

import com.sight.repository.BookBorrowRecordRepository
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import com.sight.service.dto.MyBorrowingResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
    private val bookInfoRepository: BookInfoRepository,
    private val bookItemRepository: BookItemRepository,
    private val bookBorrowRecordRepository: BookBorrowRecordRepository,
) {
    @Transactional(readOnly = true)
    fun getMyBorrowings(userId: Long): List<MyBorrowingResult> {
        val records = bookBorrowRecordRepository.findAllByUserIdAndReturnedAtIsNull(userId)
        if (records.isEmpty()) return emptyList()

        val itemIds = records.map { it.itemId }
        val itemsById = bookItemRepository.findAllById(itemIds).associateBy { it.id }

        val bookInfoIds = itemsById.values.map { it.bookInfoId }.distinct()
        val bookInfosById = bookInfoRepository.findAllById(bookInfoIds).associateBy { it.id }

        return records.map { record ->
            val item = itemsById[record.itemId] ?: return@map null
            val bookInfo = bookInfosById[item.bookInfoId] ?: return@map null
            MyBorrowingResult(
                bookId = bookInfo.id,
                itemId = item.id,
                title = bookInfo.title,
                borrowedAt = record.borrowedAt,
            )
        }.filterNotNull()
    }
}
