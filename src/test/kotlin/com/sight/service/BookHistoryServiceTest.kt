package com.sight.service

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
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.Instant
import kotlin.test.assertEquals

class BookHistoryServiceTest {
    private val bookInfoRepository: BookInfoRepository = mock()
    private val bookItemRepository: BookItemRepository = mock()
    private val bookBorrowRecordRepository: BookBorrowRecordRepository = mock()
    private val memberRepository: MemberRepository = mock()
    private lateinit var bookHistoryService: BookHistoryService

    @BeforeEach
    fun setUp() {
        bookHistoryService =
            BookHistoryService(
                bookInfoRepository = bookInfoRepository,
                bookItemRepository = bookItemRepository,
                bookBorrowRecordRepository = bookBorrowRecordRepository,
                memberRepository = memberRepository,
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

    @Test
    fun `getMyBorrowings는 대출 중인 도서가 없으면 빈 목록을 반환한다`() {
        // given
        given(bookBorrowRecordRepository.findAllByUserIdAndReturnedAtIsNull(1L)).willReturn(emptyList())

        // when
        val result = bookHistoryService.getMyBorrowings(1L)

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
        val result = bookHistoryService.getMyBorrowings(userId)

        // then
        assertEquals(1, result.size)
        assertEquals("book1", result[0].bookId)
        assertEquals("item1", result[0].itemId)
        assertEquals("테스트 도서", result[0].title)
    }

    @Test
    fun `getCurrentBorrowings는 대출 중인 기록이 없으면 빈 목록을 반환한다`() {
        // given
        given(bookBorrowRecordRepository.findAllByReturnedAtIsNullOrderByBorrowedAtDesc()).willReturn(emptyList())

        // when
        val result = bookHistoryService.getCurrentBorrowings()

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `getCurrentBorrowings는 현재 대출 중인 기록 목록을 반환한다`() {
        // given
        val bookInfo = createBookInfo("book1")
        val item = createBookItem("item1", "book1")
        val record = createBorrowRecord("record1", "item1", userId = 1L)
        val member = createMember(1L)

        given(bookBorrowRecordRepository.findAllByReturnedAtIsNullOrderByBorrowedAtDesc()).willReturn(listOf(record))
        given(bookItemRepository.findAllById(listOf("item1"))).willReturn(listOf(item))
        given(bookInfoRepository.findAllById(listOf("book1"))).willReturn(listOf(bookInfo))
        given(memberRepository.findAllById(listOf(1L))).willReturn(listOf(member))

        // when
        val result = bookHistoryService.getCurrentBorrowings()

        // then
        assertEquals(1, result.size)
        assertEquals("record1", result[0].recordId)
        assertEquals("book1", result[0].bookId)
        assertEquals(1L, result[0].borrowerUserId)
        assertEquals("홍길동", result[0].borrowerUserName)
    }

    @Test
    fun `getBorrowHistory는 대출 기록이 없으면 빈 목록을 반환한다`() {
        // given
        given(bookBorrowRecordRepository.findAllByOrderByBorrowedAtDesc()).willReturn(emptyList())

        // when
        val result = bookHistoryService.getBorrowHistory()

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `getBorrowHistory는 전체 대출 기록을 반환한다`() {
        // given
        val returnedAt = Instant.parse("2024-06-01T00:00:00Z")
        val bookInfo = createBookInfo("book1")
        val item = createBookItem("item1", "book1")
        val record = createBorrowRecord("record1", "item1", userId = 1L, returnedAt = returnedAt)
        val member = createMember(1L)

        given(bookBorrowRecordRepository.findAllByOrderByBorrowedAtDesc()).willReturn(listOf(record))
        given(bookItemRepository.findAllById(listOf("item1"))).willReturn(listOf(item))
        given(bookInfoRepository.findAllById(listOf("book1"))).willReturn(listOf(bookInfo))
        given(memberRepository.findAllById(listOf(1L))).willReturn(listOf(member))

        // when
        val result = bookHistoryService.getBorrowHistory()

        // then
        assertEquals(1, result.size)
        assertEquals("record1", result[0].recordId)
        assertEquals("book1", result[0].bookId)
        assertEquals(1L, result[0].borrowerUserId)
        assertEquals("홍길동", result[0].borrowerUserName)
        assertEquals(returnedAt, result[0].returnedAt)
    }
}
