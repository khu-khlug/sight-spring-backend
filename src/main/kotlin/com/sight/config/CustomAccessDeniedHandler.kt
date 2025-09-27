package com.sight.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.sight.controllers.http.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse =
            ErrorResponse(
                statusCode = HttpStatus.FORBIDDEN.value(),
                message = "권한이 부족합니다",
                data = null,
                timestamp = Instant.now().toEpochMilli(),
            )

        response.writer.write(objectMapper.writeValueAsString(errorResponse))
        response.writer.flush()
    }
}
