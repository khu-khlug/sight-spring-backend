package com.sight.controllers.http

import com.sight.core.auth.AuthAspect
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.BookHistoryService
import com.sight.service.dto.BorrowHistoryResult
import com.sight.service.dto.CurrentBorrowingResult
import com.sight.service.dto.MyBorrowingResult
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
    BookHistoryController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@Import(AuthAspect::class)
@EnableAspectJAutoProxy
class BookHistoryControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var bookHistoryService: BookHistoryService

    @BeforeEach
    fun setUp() {
        authenticate(UserRole.USER)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `일반 회원과 운영진 모두 내 대출 목록을 조회할 수 있다`() {
        given(bookHistoryService.getMyBorrowings(1L)).willReturn(emptyList<MyBorrowingResult>())
        mockMvc.perform(get("/book/borrowings/@me"))
            .andExpect(status().isOk)

        authenticate(UserRole.MANAGER)
        mockMvc.perform(get("/book/borrowings/@me"))
            .andExpect(status().isOk)
    }

    @Test
    fun `일반 회원은 대출 이력을 조회할 수 없고 운영진은 조회할 수 있다`() {
        mockMvc.perform(get("/book/borrow-history"))
            .andExpect(status().isForbidden)

        given(bookHistoryService.getBorrowHistory()).willReturn(emptyList<BorrowHistoryResult>())
        authenticate(UserRole.MANAGER)
        mockMvc.perform(get("/book/borrow-history"))
            .andExpect(status().isOk)
    }

    @Test
    fun `일반 회원은 현재 대출 목록을 조회할 수 없고 운영진은 조회할 수 있다`() {
        mockMvc.perform(get("/book/borrowings"))
            .andExpect(status().isForbidden)

        given(bookHistoryService.getCurrentBorrowings()).willReturn(emptyList<CurrentBorrowingResult>())
        authenticate(UserRole.MANAGER)
        mockMvc.perform(get("/book/borrowings"))
            .andExpect(status().isOk)
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
