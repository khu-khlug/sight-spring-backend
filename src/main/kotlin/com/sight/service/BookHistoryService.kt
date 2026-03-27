package com.sight.service

import com.sight.repository.BookBorrowRecordRepository
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import com.sight.repository.MemberRepository
import com.sight.service.dto.BorrowHistoryResult
import com.sight.service.dto.CurrentBorrowingResult
import com.sight.service.dto.MyBorrowingResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookHistoryService(
    private val bookInfoRepository: BookInfoRepository,
    private val bookItemRepository: BookItemRepository,
    private val bookBorrowRecordRepository: BookBorrowRecordRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional(readOnly = true)
    fun getMyBorrowings(userId: Long): List<MyBorrowingResult> {
        val records = bookBorrowRecordRepository.findAllByUserIdAndReturnedAtIsNull(userId)
        if (records.isEmpty()) return emptyList()

        val itemIds = records.map { it.itemId }
        val itemsById = bookItemRepository.findAllById(itemIds).associateBy { it.id }

        val bookInfoIds = itemsById.values.map { it.bookInfoId }.distinct()
        val bookInfosById = bookInfoRepository.findAllById(bookInfoIds).associateBy { it.id }

        return records.mapNotNull { record ->
            val item = itemsById[record.itemId] ?: return@mapNotNull null
            val bookInfo = bookInfosById[item.bookInfoId] ?: return@mapNotNull null
            MyBorrowingResult(
                bookId = bookInfo.id,
                itemId = item.id,
                title = bookInfo.title,
                borrowedAt = record.borrowedAt,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getCurrentBorrowings(): List<CurrentBorrowingResult> {
        val records = bookBorrowRecordRepository.findAllByReturnedAtIsNullOrderByBorrowedAtDesc()
        if (records.isEmpty()) return emptyList()

        val itemIds = records.map { it.itemId }
        val itemsById = bookItemRepository.findAllById(itemIds).associateBy { it.id }

        val bookInfoIds = itemsById.values.map { it.bookInfoId }.distinct()
        val bookInfosById = bookInfoRepository.findAllById(bookInfoIds).associateBy { it.id }

        val userIds = records.map { it.userId }.distinct()
        val membersById = memberRepository.findAllById(userIds).associateBy { it.id }

        return records.mapNotNull { record ->
            val item = itemsById[record.itemId] ?: return@mapNotNull null
            val bookInfo = bookInfosById[item.bookInfoId] ?: return@mapNotNull null
            val member = membersById[record.userId] ?: return@mapNotNull null
            CurrentBorrowingResult(
                recordId = record.id,
                itemId = item.id,
                bookId = bookInfo.id,
                title = bookInfo.title,
                borrowerUserId = member.id,
                borrowerUserName = member.realname,
                borrowedAt = record.borrowedAt,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getBorrowHistory(): List<BorrowHistoryResult> {
        val records = bookBorrowRecordRepository.findAllByOrderByBorrowedAtDesc()
        if (records.isEmpty()) return emptyList()

        val itemIds = records.map { it.itemId }
        val itemsById = bookItemRepository.findAllById(itemIds).associateBy { it.id }

        val bookInfoIds = itemsById.values.map { it.bookInfoId }.distinct()
        val bookInfosById = bookInfoRepository.findAllById(bookInfoIds).associateBy { it.id }

        val userIds = records.map { it.userId }.distinct()
        val membersById = memberRepository.findAllById(userIds).associateBy { it.id }

        return records.mapNotNull { record ->
            val item = itemsById[record.itemId] ?: return@mapNotNull null
            val bookInfo = bookInfosById[item.bookInfoId] ?: return@mapNotNull null
            val member = membersById[record.userId] ?: return@mapNotNull null
            BorrowHistoryResult(
                recordId = record.id,
                itemId = item.id,
                bookId = bookInfo.id,
                title = bookInfo.title,
                borrowerUserId = member.id,
                borrowerUserName = member.realname,
                borrowedAt = record.borrowedAt,
                returnedAt = record.returnedAt,
            )
        }
    }
}
