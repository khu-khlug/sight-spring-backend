package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.book.BookCategory
import com.sight.service.BookService
import com.sight.service.dto.BookStatsResult
import com.sight.service.dto.GetBookPreviewResult
import com.sight.service.dto.GetBookResult
import com.sight.service.dto.ListBookResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    BookController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class)
@EnableAspectJAutoProxy
class BookControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var bookService: BookService

    @BeforeEach
    fun setUp() {
        authenticate(UserRole.USER)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `일반 회원은 도서 통계를 조회할 수 없고 운영진은 조회할 수 있다`() {
        mockMvc.perform(get("/book/stats"))
            .andExpect(status().isForbidden)

        given(bookService.getStats()).willReturn(BookStatsResult(0, 0, 0))
        authenticate(UserRole.MANAGER)
        mockMvc.perform(get("/book/stats"))
            .andExpect(status().isOk)
    }

    @Test
    fun `일반 회원과 운영진 모두 도서 목록을 조회할 수 있다`() {
        given(bookService.listBooks()).willReturn(emptyList<ListBookResult>())
        mockMvc.perform(get("/book"))
            .andExpect(status().isOk)

        authenticate(UserRole.MANAGER)
        mockMvc.perform(get("/book"))
            .andExpect(status().isOk)
    }

    @Test
    fun `일반 회원과 운영진 모두 도서 상세를 조회할 수 있다`() {
        given(bookService.getBook("book1")).willReturn(bookResult())
        mockMvc.perform(get("/book/book1"))
            .andExpect(status().isOk)

        authenticate(UserRole.MANAGER)
        mockMvc.perform(get("/book/book1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `일반 회원과 운영진 모두 isbn으로 도서를 조회할 수 있다`() {
        given(bookService.getBookByIsbn("9780000000001")).willReturn(bookResult())
        mockMvc.perform(get("/book").param("isbn", "9780000000001"))
            .andExpect(status().isOk)

        authenticate(UserRole.MANAGER)
        mockMvc.perform(get("/book").param("isbn", "9780000000001"))
            .andExpect(status().isOk)
    }

    @Test
    fun `일반 회원은 도서 미리보기를 조회할 수 없고 운영진은 조회할 수 있다`() {
        mockMvc.perform(get("/book/preview").param("isbn", "9780000000001"))
            .andExpect(status().isForbidden)

        given(bookService.previewBook("9780000000001")).willReturn(
            GetBookPreviewResult(
                title = "테스트 도서",
                author = "저자",
                coverImageUrl = "https://example.com/cover.jpg",
                publisher = "출판사",
                publishedYear = 2024,
                description = "설명",
            ),
        )
        authenticate(UserRole.MANAGER)
        mockMvc.perform(get("/book/preview").param("isbn", "9780000000001"))
            .andExpect(status().isOk)
    }

    private fun bookResult() =
        GetBookResult(
            bookId = "book1",
            title = "테스트 도서",
            coverImageUrl = "https://example.com/cover.jpg",
            author = "저자",
            publisher = "출판사",
            publishedYear = 2024,
            totalCount = 1,
            availableCount = 1,
            isbn = "9780000000001",
            description = "설명",
            category = BookCategory.OTHER,
            itemList = emptyList(),
        )

    private fun authenticate(role: UserRole) {
        val requester = Requester(userId = 1L, role = role)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                requester,
                null,
                listOf(SimpleGrantedAuthority("ROLE_${role.name}")),
            )
    }
}
