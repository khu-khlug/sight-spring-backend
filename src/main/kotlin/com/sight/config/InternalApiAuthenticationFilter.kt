package com.sight.config
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Objects

@Component
class InternalApiAuthenticationFilter(
    @Value("\${internal.api-key:}") private val internalApiKey: String,
) : OncePerRequestFilter(), AuthenticationFilter {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.requestURI.startsWith("/internal/")) {
            val apiKeyHeader = request.getHeader("x-api-key")

            if (internalApiKey.isBlank() || apiKeyHeader.isNullOrBlank() || !Objects.equals(internalApiKey, apiKeyHeader)) {
                SecurityContextHolder.clearContext()
                filterChain.doFilter(request, response)
                return
            }

            val authorities = listOf(SimpleGrantedAuthority("ROLE_INTERNAL_API"))
            val authToken =
                UsernamePasswordAuthenticationToken(
                    "INTERNAL_API_USER",
                    null,
                    authorities,
                )
            authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authToken
        }

        filterChain.doFilter(request, response)
    }
}
