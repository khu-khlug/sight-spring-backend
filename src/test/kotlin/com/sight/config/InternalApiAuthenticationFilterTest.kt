package com.sight.config

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.core.context.SecurityContextHolder
import java.io.PrintWriter

class InternalApiAuthenticationFilterTest {
    private lateinit var filter: InternalApiAuthenticationFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain
    private lateinit var writer: PrintWriter

    @BeforeEach
    fun setUp() {
        filter = InternalApiAuthenticationFilter("test-api-key")
        request = mock(HttpServletRequest::class.java)
        response = mock(HttpServletResponse::class.java)
        filterChain = mock(FilterChain::class.java)
        writer = mock(PrintWriter::class.java)

        `when`(response.writer).thenReturn(writer)
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `x-api-key 헤더가 없는 요청은 인증 없이 필터를 통과한다`() {
        `when`(request.getHeader("x-api-key")).thenReturn(null)

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication == null)
    }

    @Test
    fun `올바른 x-api-key 헤더가 있으면 SYSTEM 역할로 인증이 성공한다`() {
        `when`(request.getHeader("x-api-key")).thenReturn("test-api-key")

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)

        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication != null)
        assert((authentication.principal as Requester).role == UserRole.SYSTEM)
        assert(authentication.authorities.any { it.authority == "ROLE_SYSTEM" })
    }

    @Test
    fun `잘못된 x-api-key 헤더가 있으면 인증하지 않고 필터를 통과한다`() {
        `when`(request.getHeader("x-api-key")).thenReturn("wrong-key")

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication == null)
    }

    @Test
    fun `internal API 키 설정이 비어있으면 인증하지 않고 필터를 통과한다`() {
        val filterWithEmptyKey = InternalApiAuthenticationFilter("")
        `when`(request.getHeader("x-api-key")).thenReturn("any-key")

        filterWithEmptyKey.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication == null)
    }

    @Test
    fun `이미 인증된 요청에는 새로운 인증을 덮어쓰지 않는다`() {
        val existingRequester = Requester(123, UserRole.MANAGER)
        val existingAuth =
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                existingRequester,
                null,
                listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MANAGER")),
            )
        SecurityContextHolder.getContext().authentication = existingAuth

        `when`(request.getHeader("x-api-key")).thenReturn("test-api-key")

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication != null)
        assert((authentication.principal as Requester).role == UserRole.MANAGER)
    }
}
