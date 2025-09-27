package com.sight.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse =
            ErrorResponse(
                statusCode = HttpStatus.UNAUTHORIZED.value(),
                message = "인증이 필요합니다",
                data = null,
                timestamp = Instant.now().toEpochMilli(),
            )

        response.writer.write(objectMapper.writeValueAsString(errorResponse))
        response.writer.flush()
    }
}
