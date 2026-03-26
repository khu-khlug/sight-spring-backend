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
        returnedAt: Instant? = null,
    ) = BookBorrowRecord(id = id, itemId = itemId, userId = 1L, returnedAt = returnedAt)

    @Test
    fun `getStats는 등록된 도서가 없으면 모든 항목이 0을 반환한다`() {
        // given
        given(bookInfoRepository.count()).willReturn(0L)
        given(bookItemRepository.count()).willReturn(0L)
        given(bookBorrowRecordRepository.countByReturnedAtIsNull()).willReturn(0L)

        // when
        val result = bookService.getStats()

        // then
        assertEquals(0L, result.totalBookCount)
        assertEquals(0L, result.totalItemCount)
        assertEquals(0L, result.currentBorrowingCount)
    }

    @Test
    fun `getStats는 도서 종수, 총 item 수, 현재 대출 중인 수를 올바르게 반환한다`() {
        // given
        given(bookInfoRepository.count()).willReturn(5L)
        given(bookItemRepository.count()).willReturn(8L)
        given(bookBorrowRecordRepository.countByReturnedAtIsNull()).willReturn(3L)

        // when
        val result = bookService.getStats()

        // then
        assertEquals(5L, result.totalBookCount)
        assertEquals(8L, result.totalItemCount)
        assertEquals(3L, result.currentBorrowingCount)
    }

    @Test
    fun `listBooks는 등록된 도서가 없으면 빈 목록을 반환한다`() {
        // given
        given(bookInfoRepository.findAll()).willReturn(emptyList())
        given(bookItemRepository.findAll()).willReturn(emptyList())
        given(bookBorrowRecordRepository.findAllByReturnedAtIsNull()).willReturn(emptyList())

        // when
        val result = bookService.listBooks()

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `listBooks는 전체 도서 목록과 totalCount, availableCount를 올바르게 반환한다`() {
        // given
        val bookInfo = createBookInfo("book1")
        val item1 = createBookItem("item1", "book1")
        val item2 = createBookItem("item2", "book1")
        val activeBorrow = createBorrowRecord("record1", "item1")

        given(bookInfoRepository.findAll()).willReturn(listOf(bookInfo))
        given(bookItemRepository.findAll()).willReturn(listOf(item1, item2))
        given(bookBorrowRecordRepository.findAllByReturnedAtIsNull()).willReturn(listOf(activeBorrow))

        // when
        val result = bookService.listBooks()

        // then
        assertEquals(1, result.size)
        assertEquals("book1", result[0].bookId)
        assertEquals(2, result[0].totalCount)
        assertEquals(1, result[0].availableCount)
    }
}
