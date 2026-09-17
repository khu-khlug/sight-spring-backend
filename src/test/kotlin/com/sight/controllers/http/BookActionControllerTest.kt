package com.sight.controllers.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.UpdateBookRequest
import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.book.BookCategory
import com.sight.service.BookActionService
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
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    BookActionController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class)
@EnableAspectJAutoProxy
class BookActionControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var bookActionService: BookActionService

    @BeforeEach
    fun setUp() {
        authenticate(UserRole.USER)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `일반 회원은 도서를 등록할 수 없고 운영진은 등록할 수 있다`() {
        mockMvc.perform(post("/book/register").param("isbn", "9780000000001"))
            .andExpect(status().isForbidden)

        given(bookActionService.registerBook("9780000000001", "OTHER", "127.0.0.1")).willReturn("book1")
        authenticate(UserRole.MANAGER)
        mockMvc.perform(
            post("/book/register")
                .param("isbn", "9780000000001")
                .param("category", "OTHER"),
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `일반 회원은 도서를 삭제할 수 없고 운영진은 삭제할 수 있다`() {
        mockMvc.perform(delete("/book/book1"))
            .andExpect(status().isForbidden)

        authenticate(UserRole.MANAGER)
        mockMvc.perform(delete("/book/book1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `일반 회원은 도서 정보를 수정할 수 없고 운영진은 수정할 수 있다`() {
        val request =
            UpdateBookRequest(
                title = "새 제목",
                author = "새 저자",
                publisher = "새 출판사",
                publishedYear = 2020,
                coverImageUrl = "https://example.com/new.jpg",
                description = "새 설명",
                category = BookCategory.MATH,
            )

        mockMvc.perform(
            put("/book/book1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)

        authenticate(UserRole.MANAGER)
        mockMvc.perform(
            put("/book/book1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `일반 회원과 운영진 모두 도서를 반납할 수 있다`() {
        mockMvc.perform(post("/book/book1/return"))
            .andExpect(status().isNoContent)

        authenticate(UserRole.MANAGER)
        mockMvc.perform(post("/book/book1/return"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `일반 회원과 운영진 모두 도서를 대출할 수 있다`() {
        mockMvc.perform(post("/book/book1/borrow"))
            .andExpect(status().isNoContent)

        authenticate(UserRole.MANAGER)
        mockMvc.perform(post("/book/book1/borrow"))
            .andExpect(status().isNoContent)
    }

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
