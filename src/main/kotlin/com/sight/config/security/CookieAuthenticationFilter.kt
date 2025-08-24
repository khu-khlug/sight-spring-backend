package com.sight.config.security

import com.sight.service.AuthService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class CookieAuthenticationFilter(
    private val authService: AuthService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val cookies = request.getHeader("Cookie")

        if (cookies != null && SecurityContextHolder.getContext().authentication == null) {
            val userId = authService.authenticate(cookies)

            if (userId != null) {
                val requester = authService.createRequester(userId)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_${requester.role.name}"))

                val authToken =
                    UsernamePasswordAuthenticationToken(
                        requester,
                        null,
                        authorities,
                    )

                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }
}
