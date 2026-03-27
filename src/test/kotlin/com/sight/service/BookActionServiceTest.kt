package com.sight.service

import com.sight.core.naver.NaverBookClient
import com.sight.core.naver.NaverBookItem
import com.sight.domain.book.BookInfo
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.InternalServerErrorException
import kotlin.test.assertEquals

class BookActionServiceTest {
    private val bookInfoRepository: BookInfoRepository = mock()
    private val bookItemRepository: BookItemRepository = mock()
    private val naverBookClient: NaverBookClient = mock()

    private val allowedSubnet = "192.168.1.0/24"
    private val allowedIp = "192.168.1.100"
    private val blockedIp = "10.0.0.1"

    private val bookActionService =
        BookActionService(
            bookInfoRepository = bookInfoRepository,
            bookItemRepository = bookItemRepository,
            naverBookClient = naverBookClient,
            allowedNetIp = allowedSubnet,
        )

    private fun createBookInfo(isbn: String = "9780000000001") =
        BookInfo(
            id = "book1",
            isbn = isbn,
            title = "테스트 도서",
            author = "저자",
            publisher = "출판사",
            publishedYear = 2024,
            coverImageUrl = "https://example.com/cover.jpg",
            description = "설명",
        )

    private fun createNaverBookItem() =
        NaverBookItem(
            title = "테스트 도서",
            author = "저자",
            publisher = "출판사",
            pubdate = "20240101",
            image = "https://example.com/cover.jpg",
            description = "설명",
        )

    @Test
    fun `registerBook은 동방 와이파이가 아닌 IP에서 요청하면 에러가 발생한다`() {
        // given
        val isbn = "9780000000001"

        // when & then
        assertThrows<ForbiddenException> {
            bookActionService.registerBook(isbn, blockedIp)
        }
    }

    @Test
    fun `registerBook은 isbn이 13자리가 아니면 에러가 발생한다`() {
        // given
        val isbn = "12345"

        // when & then
        assertThrows<BadRequestException> {
            bookActionService.registerBook(isbn, allowedIp)
        }
    }

    @Test
    fun `registerBook은 isbn이 DB에 이미 존재하면 기존 BookInfo에 BookItem을 추가하고 bookId를 반환한다`() {
        // given
        val isbn = "9780000000001"
        val bookInfo = createBookInfo(isbn)
        given(bookInfoRepository.findByIsbn(isbn)).willReturn(bookInfo)

        // when
        val result = bookActionService.registerBook(isbn, allowedIp)

        // then
        assertEquals("book1", result)
        verify(bookItemRepository).save(any())
    }

    @Test
    fun `registerBook은 isbn이 DB에 없고 네이버 조회가 가능하면 BookInfo와 BookItem을 생성하고 bookId를 반환한다`() {
        // given
        val isbn = "9780000000001"
        given(bookInfoRepository.findByIsbn(isbn)).willReturn(null)
        given(naverBookClient.searchByIsbn(isbn)).willReturn(createNaverBookItem())

        // when
        val result = bookActionService.registerBook(isbn, allowedIp)

        // then
        verify(bookInfoRepository).save(any())
        verify(bookItemRepository).save(any())
        assertEquals(result.length, 26) // ULID 길이
    }

    @Test
    fun `registerBook은 isbn이 DB에 없고 네이버 조회가 불가능하면 에러가 발생한다`() {
        // given
        val isbn = "9780000000001"
        given(bookInfoRepository.findByIsbn(isbn)).willReturn(null)
        given(naverBookClient.searchByIsbn(isbn)).willReturn(null)

        // when & then
        assertThrows<InternalServerErrorException> {
            bookActionService.registerBook(isbn, allowedIp)
        }
    }
}
