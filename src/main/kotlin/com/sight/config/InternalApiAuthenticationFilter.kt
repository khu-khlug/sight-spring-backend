package com.sight.config
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
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
        val apiKeyHeader = request.getHeader("x-api-key")

        if (!internalApiKey.isBlank() &&
            !apiKeyHeader.isNullOrBlank() &&
            Objects.equals(internalApiKey, apiKeyHeader) &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            val requester = Requester(-1, UserRole.SYSTEM)
            val authorities = listOf(SimpleGrantedAuthority("ROLE_SYSTEM"))
            val authToken =
                UsernamePasswordAuthenticationToken(
                    requester,
                    null,
                    authorities,
                )
            authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authToken
        }

        filterChain.doFilter(request, response)
    }
}
