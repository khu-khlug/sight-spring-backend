package com.sight.service

import com.sight.repository.BookBorrowRecordRepository
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
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
}
