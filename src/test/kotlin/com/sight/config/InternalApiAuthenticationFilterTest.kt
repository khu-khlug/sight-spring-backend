package com.sight.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
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
    fun `internal 경로가 아닌 요청은 필터를 통과한다`() {
        `when`(request.requestURI).thenReturn("/ping")

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        verify(response, never()).status = HttpStatus.UNAUTHORIZED.value()
    }

    @Test
    fun `internal 경로에 올바른 API 키가 있으면 인증이 성공한다`() {
        `when`(request.requestURI).thenReturn("/internal/test")
        `when`(request.getHeader("x-api-key")).thenReturn("test-api-key")

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        verify(response, never()).status = HttpStatus.UNAUTHORIZED.value()

        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication != null)
        assert(authentication.principal == "INTERNAL_API_USER")
        assert(authentication.authorities.any { it.authority == "ROLE_INTERNAL_API" })
    }

    @Test
    fun `internal 경로에 API 키가 없으면 SecurityContext를 비우고 필터 체인을 계속한다`() {
        `when`(request.requestURI).thenReturn("/internal/test")
        `when`(request.getHeader("x-api-key")).thenReturn(null)

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication == null)
    }

    @Test
    fun `internal 경로에 잘못된 API 키가 있으면 SecurityContext를 비우고 필터 체인을 계속한다`() {
        `when`(request.requestURI).thenReturn("/internal/test")
        `when`(request.getHeader("x-api-key")).thenReturn("wrong-key")

        filter.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication == null)
    }

    @Test
    fun `internal API 키 설정이 비어있으면 SecurityContext를 비우고 필터 체인을 계속한다`() {
        val filterWithEmptyKey = InternalApiAuthenticationFilter("")
        `when`(request.requestURI).thenReturn("/internal/test")
        `when`(request.getHeader("x-api-key")).thenReturn("any-key")

        filterWithEmptyKey.doFilterInternal(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        val authentication = SecurityContextHolder.getContext().authentication
        assert(authentication == null)
    }
}
