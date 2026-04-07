package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.naver.NaverBookClient
import com.sight.core.naver.NaverBookItem
import com.sight.domain.book.BookBorrowRecord
import com.sight.domain.book.BookInfo
import com.sight.domain.book.BookItem
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
import com.sight.domain.member.UserStatus
import com.sight.repository.BookBorrowRecordRepository
import com.sight.repository.BookInfoRepository
import com.sight.repository.BookItemRepository
import com.sight.repository.MemberRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BookServiceTest {
    private val bookInfoRepository: BookInfoRepository = mock()
    private val bookItemRepository: BookItemRepository = mock()
    private val bookBorrowRecordRepository: BookBorrowRecordRepository = mock()
    private val memberRepository: MemberRepository = mock()
    private val naverBookClient: NaverBookClient = mock()
    private lateinit var bookService: BookService

    @BeforeEach
    fun setUp() {
        bookService =
            BookService(
                bookInfoRepository = bookInfoRepository,
                bookItemRepository = bookItemRepository,
                bookBorrowRecordRepository = bookBorrowRecordRepository,
                memberRepository = memberRepository,
                naverBookClient = naverBookClient,
            )
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
        bookInfoId: String,
    ) = BookItem(id = id, bookInfoId = bookInfoId)

    private fun createBorrowRecord(
        id: String,
        itemId: String,
        userId: Long = 1L,
        returnedAt: Instant? = null,
    ) = BookBorrowRecord(id = id, itemId = itemId, userId = userId, returnedAt = returnedAt)

    private fun createMember(id: Long = 1L) =
        Member(
            id = id,
            name = "testuser",
            realname = "홍길동",
            admission = "19",
            college = "공과대학",
            grade = 3L,
            studentStatus = StudentStatus.UNDERGRADUATE,
            status = UserStatus.ACTIVE,
        )

    // getStats

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

    // listBooks

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

    // getBook

    @Test
    fun `getBook은 해당 bookId의 도서가 없으면 404 예외를 던진다`() {
        // given
        given(bookInfoRepository.findById("unknown")).willReturn(Optional.empty())

        // then
        assertThrows<NotFoundException> {
            bookService.getBook("unknown")
        }
    }

    @Test
    fun `getBook은 모든 item이 대출 중이지 않으면 borrowerInfo가 null이다`() {
        // given
        val bookInfo = createBookInfo("book1")
        val item1 = createBookItem("item1", "book1")
        val item2 = createBookItem("item2", "book1")

        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item1, item2))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1", "item2")))
            .willReturn(emptyList())
        given(memberRepository.findAllById(emptyList())).willReturn(emptyList())

        // when
        val result = bookService.getBook("book1")

        // then
        assertEquals(2, result.totalCount)
        assertEquals(2, result.availableCount)
        assertNull(result.itemList[0].borrowerInfo)
        assertNull(result.itemList[1].borrowerInfo)
    }

    @Test
    fun `getBook은 일부 item이 대출 중이면 해당 item의 borrowerInfo를 포함한다`() {
        // given
        val bookInfo = createBookInfo("book1")
        val item1 = createBookItem("item1", "book1")
        val item2 = createBookItem("item2", "book1")
        val member = createMember(1L)
        val borrowedAt = Instant.parse("2024-06-01T00:00:00Z")
        val borrow =
            createBorrowRecord("record1", "item1", userId = 1L)
                .copy(borrowedAt = borrowedAt)

        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item1, item2))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1", "item2")))
            .willReturn(listOf(borrow))
        given(memberRepository.findAllById(listOf(1L))).willReturn(listOf(member))

        // when
        val result = bookService.getBook("book1")

        // then
        assertEquals(2, result.totalCount)
        assertEquals(1, result.availableCount)
        assertNotNull(result.itemList.first { it.itemId == "item1" }.borrowerInfo).also {
            assertEquals(1L, it.borrowerUserId)
            assertEquals("홍길동", it.borrowerUserName)
            assertEquals(borrowedAt, it.borrowedAt)
        }
        assertNull(result.itemList.first { it.itemId == "item2" }.borrowerInfo)
    }

    @Test
    fun `getBook은 모든 item이 대출 중이면 availableCount가 0이다`() {
        // given
        val bookInfo = createBookInfo("book1")
        val item1 = createBookItem("item1", "book1")
        val item2 = createBookItem("item2", "book1")
        val member1 = createMember(1L)
        val member2 = createMember(2L).copy(name = "testuser2", realname = "김철수")
        val borrow1 = createBorrowRecord("record1", "item1", userId = 1L)
        val borrow2 = createBorrowRecord("record2", "item2", userId = 2L)

        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item1, item2))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1", "item2")))
            .willReturn(listOf(borrow1, borrow2))
        given(memberRepository.findAllById(listOf(1L, 2L))).willReturn(listOf(member1, member2))

        // when
        val result = bookService.getBook("book1")

        // then
        assertEquals(2, result.totalCount)
        assertEquals(0, result.availableCount)
        assertNotNull(result.itemList.first { it.itemId == "item1" }.borrowerInfo)
        assertNotNull(result.itemList.first { it.itemId == "item2" }.borrowerInfo)
    }

    // getBookByIsbn

    @Test
    fun `getBookByIsbn은 해당 isbn의 도서가 없으면 404 예외를 던진다`() {
        // given
        given(bookInfoRepository.findByIsbn("0000000000000")).willReturn(null)

        // then
        assertThrows<NotFoundException> {
            bookService.getBookByIsbn("0000000000000")
        }
    }

    @Test
    fun `getBookByIsbn은 getBook과 동일한 결과를 반환한다`() {
        // given
        val bookInfo = createBookInfo("book1", isbn = "9780000000001")
        val item1 = createBookItem("item1", "book1")
        val member = createMember(1L)
        val borrow = createBorrowRecord("record1", "item1", userId = 1L)

        given(bookInfoRepository.findByIsbn("9780000000001")).willReturn(bookInfo)
        given(bookInfoRepository.findById("book1")).willReturn(Optional.of(bookInfo))
        given(bookItemRepository.findAllByBookInfoId("book1")).willReturn(listOf(item1))
        given(bookBorrowRecordRepository.findAllByItemIdInAndReturnedAtIsNull(listOf("item1")))
            .willReturn(listOf(borrow))
        given(memberRepository.findAllById(listOf(1L))).willReturn(listOf(member))

        // when
        val byIsbn = bookService.getBookByIsbn("9780000000001")
        val byId = bookService.getBook("book1")

        // then
        assertEquals(byId, byIsbn)
    }

    // previewBook 테스트

    @Test
    fun `isbn이 13자리가 아니면 예외가 발생한다`() {
        assertThrows<BadRequestException> {
            bookService.previewBook("123")
        }
    }

    @Test
    fun `해당 isbn의 도서가 DB에 존재하면 DB 정보를 반환한다`() {
        // given
        val bookInfo = createBookInfo(isbn = "9780000000001")
        given(bookInfoRepository.findByIsbn("9780000000001")).willReturn(bookInfo)

        // when
        val result = bookService.previewBook("9780000000001")

        // then
        assertEquals(bookInfo.title, result.title)
        assertEquals(bookInfo.author, result.author)
        assertEquals(bookInfo.coverImageUrl, result.coverImageUrl)
        assertEquals(bookInfo.publisher, result.publisher)
        assertEquals(bookInfo.publishedYear, result.publishedYear)
        assertEquals(bookInfo.description, result.description)
    }

    @Test
    fun `해당 isbn의 도서가 DB에 없고 외부 조회가 가능하면 외부 API 정보를 반환한다`() {
        // given
        val isbn = "9780000000001"
        val naverItem =
            NaverBookItem(
                title = "네이버 도서",
                author = "네이버 저자",
                publisher = "네이버 출판사",
                pubdate = "20240101",
                image = "https://example.com/cover.jpg",
                description = "네이버 설명",
            )
        given(bookInfoRepository.findByIsbn(isbn)).willReturn(null)
        given(naverBookClient.searchByIsbn(isbn)).willReturn(naverItem)

        // when
        val result = bookService.previewBook(isbn)

        // then
        assertEquals(naverItem.title, result.title)
        assertEquals(naverItem.author, result.author)
        assertEquals(naverItem.image, result.coverImageUrl)
        assertEquals(naverItem.publisher, result.publisher)
        assertEquals(2024, result.publishedYear)
        assertEquals(naverItem.description, result.description)
    }

    @Test
    fun `해당 isbn의 도서가 DB에 없고 외부 조회도 불가능하면 예외가 발생한다`() {
        // given
        val isbn = "9780000000001"
        given(bookInfoRepository.findByIsbn(isbn)).willReturn(null)
        given(naverBookClient.searchByIsbn(isbn)).willReturn(null)

        // then
        assertThrows<NotFoundException> {
            bookService.previewBook(isbn)
        }
    }
}
