package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.InternalServerErrorException
import com.sight.core.exception.NotFoundException
import com.sight.core.naver.NaverBookClient
import com.sight.domain.book.BookBorrowRecord
import com.sight.domain.book.BookInfo
import com.sight.domain.book.BookItem
import com.sight.repository.BookBorrowRecordRepository
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.nio.ByteBuffer
import java.time.Instant

@Service
class BookActionService(
    private val bookInfoRepository: BookInfoRepository,
    private val bookItemRepository: BookItemRepository,
    private val bookBorrowRecordRepository: BookBorrowRecordRepository,
    private val naverBookClient: NaverBookClient,
    @Value("\${book.scan.allowed-net-ip}") private val allowedNetIp: String,
) {
    @Transactional
    fun registerBook(
        isbn: String,
        clientIp: String,
    ): String {
        if (!isAllowedIp(clientIp)) {
            throw ForbiddenException("동아리방 와이파이에서만 등록할 수 있습니다")
        }
        if (isbn.length != 13) {
            throw BadRequestException("isbn은 13자리여야 합니다")
        }

        val existingBookInfo = bookInfoRepository.findByIsbn(isbn)
        val bookInfoId =
            if (existingBookInfo != null) {
                existingBookInfo.id
            } else {
                val naverItem =
                    naverBookClient.searchByIsbn(isbn)
                        ?: throw InternalServerErrorException("외부 도서 정보를 조회할 수 없습니다")
                val newBookInfo =
                    BookInfo(
                        id = UlidCreator.getUlid().toString(),
                        isbn = isbn,
                        title = naverItem.title,
                        author = naverItem.author,
                        publisher = naverItem.publisher,
                        publishedYear = naverItem.pubdate.take(4).toIntOrNull() ?: 0,
                        coverImageUrl = naverItem.image,
                        description = naverItem.description,
                    )
                bookInfoRepository.save(newBookInfo)
                newBookInfo.id
            }

        val newItem =
            BookItem(
                id = UlidCreator.getUlid().toString(),
                bookInfoId = bookInfoId,
            )
        bookItemRepository.save(newItem)

        return bookInfoId
    }

    @Transactional
    fun deleteBook(bookId: String) {
        bookInfoRepository.findById(bookId).orElseThrow {
            NotFoundException("도서를 찾을 수 없습니다")
        }

        val allItems = bookItemRepository.findAllByBookInfoId(bookId)
        val borrowedItemIds =
            bookBorrowRecordRepository
                .findAllByItemIdInAndReturnedAtIsNull(allItems.map { it.id })
                .map { it.itemId }
                .toSet()

        val availableItems = allItems.filter { it.id !in borrowedItemIds }

        if (availableItems.isEmpty()) {
            throw BadRequestException("모든 item이 대출 중입니다")
        }

        val itemToDelete =
            if (availableItems.size == 1) {
                availableItems.first()
            } else {
                availableItems.maxBy { it.createdAt }
            }

        bookItemRepository.delete(itemToDelete)

        if (allItems.size == 1) {
            bookInfoRepository.deleteById(bookId)
        }
    }

    @Transactional
    fun returnBook(
        bookId: String,
        userId: Long,
        clientIp: String,
    ) {
        bookInfoRepository.findById(bookId).orElseThrow {
            NotFoundException("도서를 찾을 수 없습니다")
        }

        if (!isAllowedIp(clientIp)) {
            throw ForbiddenException("동아리방 와이파이에서만 반납할 수 있습니다")
        }

        val itemIds = bookItemRepository.findAllByBookInfoId(bookId).map { it.id }
        val record =
            bookBorrowRecordRepository.findByUserIdAndItemIdInAndReturnedAtIsNull(userId, itemIds)
                ?: throw BadRequestException("해당 도서를 대출 중이 아닙니다")

        bookBorrowRecordRepository.save(record.copy(returnedAt = Instant.now()))
    }

    @Transactional
    fun borrowBook(
        bookId: String,
        userId: Long,
        clientIp: String,
    ) {
        bookInfoRepository.findById(bookId).orElseThrow {
            NotFoundException("도서를 찾을 수 없습니다")
        }
        
        if (!isAllowedIp(clientIp)) {
            throw ForbiddenException("동아리방 와이파이에서만 대출할 수 있습니다")
        }

        val allItems = bookItemRepository.findAllByBookInfoId(bookId)
        val borrowedItemIds =
            bookBorrowRecordRepository
                .findAllByItemIdInAndReturnedAtIsNull(allItems.map { it.id })
                .map { it.itemId }
                .toSet()

        val availableItems = allItems.filter { it.id !in borrowedItemIds }
        if (availableItems.isEmpty()) {
            throw BadRequestException("대출 가능한 item이 없습니다")
        }

        val alreadyBorrowing =
            bookBorrowRecordRepository.findByUserIdAndItemIdInAndReturnedAtIsNull(userId, allItems.map { it.id })
        if (alreadyBorrowing != null) {
            throw BadRequestException("이미 해당 도서를 대출 중입니다")
        }

        

        val itemToBorrow = availableItems.minBy { it.createdAt }
        bookBorrowRecordRepository.save(
            BookBorrowRecord(
                id = UlidCreator.getUlid().toString(),
                itemId = itemToBorrow.id,
                userId = userId,
            ),
        )
    }

    private fun isAllowedIp(clientIp: String): Boolean {
        if (allowedNetIp.isBlank()) return false
        return try {
            val (subnetStr, prefixLenStr) = allowedNetIp.split("/")
            val prefixLen = prefixLenStr.toInt()
            val mask = if (prefixLen == 0) 0 else (-1 shl (32 - prefixLen))
            val subnetInt = ByteBuffer.wrap(InetAddress.getByName(subnetStr).address).int
            val ipInt = ByteBuffer.wrap(InetAddress.getByName(clientIp).address).int
            (ipInt and mask) == (subnetInt and mask)
        } catch (e: Exception) {
            false
        }
    }
}
