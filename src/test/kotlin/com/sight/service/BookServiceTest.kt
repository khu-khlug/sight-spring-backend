package com.sight.service

import com.sight.domain.book.BookBorrowRecord
import com.sight.domain.book.BookInfo
import com.sight.domain.book.BookItem
import com.sight.repository.BookBorrowRecordRepository
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.Instant
import kotlin.test.assertEquals

class BookServiceTest {
    private val bookInfoRepository: BookInfoRepository = mock()
    private val bookItemRepository: BookItemRepository = mock()
    private val bookBorrowRecordRepository: BookBorrowRecordRepository = mock()
    private lateinit var bookService: BookService

    @BeforeEach
    fun setUp() {
        bookService =
            BookService(
                bookInfoRepository = bookInfoRepository,
                bookItemRepository = bookItemRepository,
                bookBorrowRecordRepository = bookBorrowRecordRepository,
            )
    }

    private fun createBookInfo(id: String = "book1") =
        BookInfo(
            id = id,
            isbn = "9780000000001",
            title = "테스트 도서",
            author = "저자",
            publisher = "출판사",
            publishedYear = 2024,
            coverImageUrl = "https://example.com/cover.jpg",
            description = "설명",
        )

    private fun createBookItem(
        id: String,
        bookInfoId: String,
    ) = BookItem(id = id, bookInfoId = bookInfoId)

    private fun createBorrowRecord(
        id: String,
        itemId: String,
        userId: Long = 1L,
        returnedAt: Instant? = null,
    ) = BookBorrowRecord(id = id, itemId = itemId, userId = userId, returnedAt = returnedAt)

    @Test
    fun `getMyBorrowings는 대출 중인 도서가 없으면 빈 목록을 반환한다`() {
        // given
        given(bookBorrowRecordRepository.findAllByUserIdAndReturnedAtIsNull(1L)).willReturn(emptyList())

        // when
        val result = bookService.getMyBorrowings(1L)

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `getMyBorrowings는 사용자의 현재 대출 목록을 반환한다`() {
        // given
        val userId = 1L
        val bookInfo = createBookInfo("book1")
        val item = createBookItem("item1", "book1")
        val record = createBorrowRecord("record1", "item1", userId)

        given(bookBorrowRecordRepository.findAllByUserIdAndReturnedAtIsNull(userId)).willReturn(listOf(record))
        given(bookItemRepository.findAllById(listOf("item1"))).willReturn(listOf(item))
        given(bookInfoRepository.findAllById(listOf("book1"))).willReturn(listOf(bookInfo))

        // when
        val result = bookService.getMyBorrowings(userId)

        // then
        assertEquals(1, result.size)
        assertEquals("book1", result[0].bookId)
        assertEquals("item1", result[0].itemId)
        assertEquals("테스트 도서", result[0].title)
    }
}
