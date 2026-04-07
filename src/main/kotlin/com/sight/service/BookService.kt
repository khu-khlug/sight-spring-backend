package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.naver.NaverBookClient
import com.sight.domain.book.BookInfo
import com.sight.repository.BookBorrowRecordRepository
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import com.sight.repository.MemberRepository
import com.sight.service.dto.BookStatsResult
import com.sight.service.dto.GetBookBorrowerInfoResult
import com.sight.service.dto.GetBookItemResult
import com.sight.service.dto.GetBookPreviewResult
import com.sight.service.dto.GetBookResult
import com.sight.service.dto.ListBookResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
    private val bookInfoRepository: BookInfoRepository,
    private val bookItemRepository: BookItemRepository,
    private val bookBorrowRecordRepository: BookBorrowRecordRepository,
    private val memberRepository: MemberRepository,
    private val naverBookClient: NaverBookClient,
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

    @Transactional(readOnly = true)
    fun getBook(bookId: String): GetBookResult {
        val bookInfo =
            bookInfoRepository.findById(bookId).orElseThrow {
                NotFoundException("도서를 찾을 수 없습니다")
            }
        return buildBookDetail(bookInfo)
    }

    @Transactional(readOnly = true)
    fun getBookByIsbn(isbn: String): GetBookResult {
        val bookInfo =
            bookInfoRepository.findByIsbn(isbn)
                ?: throw NotFoundException("도서를 찾을 수 없습니다")
        return buildBookDetail(bookInfo)
    }

    @Transactional(readOnly = true)
    fun previewBook(isbn: String): GetBookPreviewResult {
        if (isbn.length != 13) {
            throw BadRequestException("isbn은 13자리여야 합니다")
        }
        val existing = bookInfoRepository.findByIsbn(isbn)
        if (existing != null) {
            return GetBookPreviewResult(
                title = existing.title,
                author = existing.author,
                coverImageUrl = existing.coverImageUrl,
                publisher = existing.publisher,
                publishedYear = existing.publishedYear,
                description = existing.description,
            )
        }
        val naverItem =
            naverBookClient.searchByIsbn(isbn)
                ?: throw NotFoundException("도서 정보를 찾을 수 없습니다")
        return GetBookPreviewResult(
            title = naverItem.title,
            author = naverItem.author,
            coverImageUrl = naverItem.image,
            publisher = naverItem.publisher,
            publishedYear = naverItem.pubdate.take(4).toIntOrNull() ?: 0,
            description = naverItem.description,
        )
    }

    private fun buildBookDetail(bookInfo: BookInfo): GetBookResult {
        val items = bookItemRepository.findAllByBookInfoId(bookInfo.id)
        val itemIds = items.map { it.id }
        val activeBorrowByItemId =
            bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(itemIds)
                .associateBy { it.itemId }
        val borrowerUserIds = activeBorrowByItemId.values.map { it.userId }.distinct()
        val membersById = memberRepository.findAllById(borrowerUserIds).associateBy { it.id }

        val totalCount = items.size
        val availableCount = items.count { it.id !in activeBorrowByItemId }

        val itemList =
            items.map { item ->
                val borrow = activeBorrowByItemId[item.id]
                val member = borrow?.let { membersById[it.userId] }
                val borrowerInfo =
                    if (borrow != null) {
                        GetBookBorrowerInfoResult(
                            borrowerUserId = member?.id ?: borrow.userId,
                            borrowerUserName = member?.realname ?: "",
                            borrowedAt = borrow.borrowedAt,
                        )
                    } else {
                        null
                    }
                GetBookItemResult(
                    itemId = item.id,
                    registeredAt = item.createdAt,
                    borrowerInfo = borrowerInfo,
                )
            }

        return GetBookResult(
            bookId = bookInfo.id,
            title = bookInfo.title,
            coverImageUrl = bookInfo.coverImageUrl,
            author = bookInfo.author,
            publisher = bookInfo.publisher,
            publishedYear = bookInfo.publishedYear,
            totalCount = totalCount,
            availableCount = availableCount,
            isbn = bookInfo.isbn,
            description = bookInfo.description,
            itemList = itemList,
        )
    }
}
