package com.sight.service

import com.sight.core.config.ConfigKey
import com.sight.core.config.SystemConfigRegistry
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.InternalServerErrorException
import com.sight.core.exception.NotFoundException
import com.sight.core.naver.NaverBookClient
import com.sight.core.naver.NaverBookItem
import com.sight.domain.book.BookBorrowRecord
import com.sight.domain.book.BookInfo
import com.sight.domain.book.BookItem
import com.sight.repository.BookBorrowRecordRepository
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals

class BookActionServiceTest {
    private val bookInfoRepository: BookInfoRepository = mock()
    private val bookItemRepository: BookItemRepository = mock()
    private val bookBorrowRecordRepository: BookBorrowRecordRepository = mock()
    private val naverBookClient: NaverBookClient = mock()
    private val systemConfigRegistry: SystemConfigRegistry = mock()

    private val allowedIp = "192.168.1.100"
    private val blockedIp = "10.0.0.1"

    private val bookActionService =
        BookActionService(
            bookInfoRepository = bookInfoRepository,
            bookItemRepository = bookItemRepository,
            bookBorrowRecordRepository = bookBorrowRecordRepository,
            naverBookClient = naverBookClient,
            systemConfigRegistry = systemConfigRegistry,
        )

    @BeforeEach
    fun setUp() {
        given(systemConfigRegistry.getValue(ConfigKey.BOOK_SCAN_ALLOWED_NET_IP)).willReturn(allowedIp)
    }

    private fun createBookInfo(
        id: String = "book1",
        isbn: String = "9780000000001",
    ) = BookInfo(
        id = id,
        isbn = isbn,
        title = "테스트 도서",
        author = "저자",
        publisher = "출판사",
        publishedYear = 2024,
        coverImageUrl = "https://example.com/cover.jpg",
        description = "설명",
    )

    private fun createBookItem(
        id: String,
        bookInfoId: String = "book1",
        createdAt: Instant = Instant.now(),
    ) = BookItem(id = id, bookInfoId = bookInfoId, createdAt = createdAt)

    private fun createBorrowRecord(itemId: String) =
        BookBorrowRecord(
            id = "record-$itemId",
            itemId = itemId,
            userId = 1L,
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
        val bookInfo = createBookInfo(isbn = isbn)
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

    @Test
    fun `deleteBook은 존재하지 않는 bookId로 요청하면 에러가 발생한다`() {
        given(bookInfoRepository.findById("nonexistent")).willReturn(Optional.empty())

        assertThrows<NotFoundException> {
            bookActionService.deleteBook("nonexistent")
        }
    }

    @Test
    fun `deleteBook은 모든 item이 대출 중이면 에러가 발생한다`() {
        // given
        val bookInfo = createBookInfo()
        val item = createBookItem("item1")
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1")))
            .willReturn(listOf(createBorrowRecord("item1")))

        // when & then
        assertThrows<BadRequestException> {
            bookActionService.deleteBook("book1")
        }
    }

    @Test
    fun `deleteBook은 미대출 item이 하나이고 전체 item도 하나이면 item과 BookInfo를 모두 삭제한다`() {
        // given
        val bookInfo = createBookInfo()
        val item = createBookItem("item1")
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1")))
            .willReturn(emptyList())

        // when
        bookActionService.deleteBook("book1")

        // then
        verify(bookItemRepository).delete(item)
        verify(bookInfoRepository).deleteById("book1")
    }

    @Test
    fun `deleteBook은 미대출 item이 하나이고 전체 item이 둘 이상이면 해당 item만 삭제하고 BookInfo는 유지한다`() {
        // given
        val bookInfo = createBookInfo()
        val item1 = createBookItem("item1")
        val item2 = createBookItem("item2")
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item1, item2))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1", "item2")))
            .willReturn(listOf(createBorrowRecord("item1")))

        // when
        bookActionService.deleteBook("book1")

        // then
        verify(bookItemRepository).delete(item2)
        verify(bookInfoRepository, never()).deleteById(any())
    }

    @Test
    fun `deleteBook은 미대출 item이 둘 이상이면 가장 최근 등록된 item을 삭제한다`() {
        // given
        val bookInfo = createBookInfo()
        val olderItem = createBookItem("item1", createdAt = Instant.parse("2024-01-01T00:00:00Z"))
        val newerItem = createBookItem("item2", createdAt = Instant.parse("2024-06-01T00:00:00Z"))
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(olderItem, newerItem))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1", "item2")))
            .willReturn(emptyList())

        // when
        bookActionService.deleteBook("book1")

        // then
        verify(bookItemRepository).delete(newerItem)
        verify(bookInfoRepository, never()).deleteById(any())
    }

    @Test
    fun `returnBook은 존재하지 않는 bookId로 요청하면 에러가 발생한다`() {
        given(bookInfoRepository.findById("nonexistent")).willReturn(Optional.empty())

        assertThrows<NotFoundException> {
            bookActionService.returnBook("nonexistent", 1L, allowedIp)
        }
    }

    @Test
    fun `returnBook은 동방 와이파이가 아닌 IP에서 요청하면 에러가 발생한다`() {
        // given
        val bookInfo = createBookInfo()
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))

        // when & then
        assertThrows<ForbiddenException> {
            bookActionService.returnBook("book1", 1L, blockedIp)
        }
    }

    @Test
    fun `returnBook은 사용자가 해당 도서를 대출 중이 아니면 에러가 발생한다`() {
        // given
        val bookInfo = createBookInfo()
        val item = createBookItem("item1")
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item))
        given(bookBorrowRecordRepository.findByUserIdAndItemIdInAndReturnedAtIsNull(1L, listOf("item1")))
            .willReturn(null)

        // when & then
        assertThrows<BadRequestException> {
            bookActionService.returnBook("book1", 1L, allowedIp)
        }
    }

    @Test
    fun `returnBook은 요청이 적절하면 returnedAt을 갱신한다`() {
        // given
        val bookInfo = createBookInfo()
        val item = createBookItem("item1")
        val record = createBorrowRecord("item1")
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item))
        given(bookBorrowRecordRepository.findByUserIdAndItemIdInAndReturnedAtIsNull(1L, listOf("item1")))
            .willReturn(record)

        // when
        bookActionService.returnBook("book1", 1L, allowedIp)

        // then
        verify(bookBorrowRecordRepository).save(
            org.mockito.kotlin.argThat { returnedAt != null },
        )
    }

    @Test
    fun `borrowBook은 존재하지 않는 bookId로 요청하면 에러가 발생한다`() {
        given(bookInfoRepository.findById("nonexistent")).willReturn(Optional.empty())

        assertThrows<NotFoundException> {
            bookActionService.borrowBook("nonexistent", 1L, allowedIp)
        }
    }

    @Test
    fun `borrowBook은 대출 가능한 item이 없으면 에러가 발생한다`() {
        // given
        val bookInfo = createBookInfo()
        val item = createBookItem("item1")
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1")))
            .willReturn(listOf(createBorrowRecord("item1")))

        // when & then
        assertThrows<BadRequestException> {
            bookActionService.borrowBook("book1", 1L, allowedIp)
        }
    }

    @Test
    fun `borrowBook은 사용자가 해당 도서를 이미 대출 중이면 에러가 발생한다`() {
        // given
        val bookInfo = createBookInfo()
        val item1 = createBookItem("item1")
        val item2 = createBookItem("item2")
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item1, item2))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1", "item2")))
            .willReturn(listOf(createBorrowRecord("item1")))
        given(bookBorrowRecordRepository.findByUserIdAndItemIdInAndReturnedAtIsNull(1L, listOf("item1", "item2")))
            .willReturn(createBorrowRecord("item1"))

        // when & then
        assertThrows<BadRequestException> {
            bookActionService.borrowBook("book1", 1L, allowedIp)
        }
    }

    @Test
    fun `borrowBook은 동방 와이파이가 아닌 IP에서 요청하면 에러가 발생한다`() {
        // given
        val bookInfo = createBookInfo()
        val item = createBookItem("item1")
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1")))
            .willReturn(emptyList())
        given(bookBorrowRecordRepository.findByUserIdAndItemIdInAndReturnedAtIsNull(1L, listOf("item1")))
            .willReturn(null)

        // when & then
        assertThrows<ForbiddenException> {
            bookActionService.borrowBook("book1", 1L, blockedIp)
        }
    }

    @Test
    fun `borrowBook은 요청이 적절하면 대출 기록을 생성한다`() {
        // given
        val bookInfo = createBookInfo()
        val item = createBookItem("item1")
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1")))
            .willReturn(emptyList())
        given(bookBorrowRecordRepository.findByUserIdAndItemIdInAndReturnedAtIsNull(1L, listOf("item1")))
            .willReturn(null)

        // when
        bookActionService.borrowBook("book1", 1L, allowedIp)

        // then
        verify(bookBorrowRecordRepository).save(
            org.mockito.kotlin.argThat { itemId == "item1" && userId == 1L && returnedAt == null },
        )
    }
}
