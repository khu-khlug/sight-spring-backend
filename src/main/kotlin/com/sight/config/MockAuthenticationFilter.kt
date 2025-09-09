package com.sight.config

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Profile("local")
class MockAuthenticationFilter : OncePerRequestFilter(), AuthenticationFilter {
    private val mockUserId = 1L
    private val mockUserRole = UserRole.MANAGER

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (SecurityContextHolder.getContext().authentication == null) {
            val mockRequester = Requester(mockUserId, mockUserRole)
            val authorities = listOf(SimpleGrantedAuthority("ROLE_${mockUserRole.name}"))

            val authToken =
                UsernamePasswordAuthenticationToken(
                    mockRequester,
                    null,
                    authorities,
                )

            authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authToken
        }

        filterChain.doFilter(request, response)
    }
}
