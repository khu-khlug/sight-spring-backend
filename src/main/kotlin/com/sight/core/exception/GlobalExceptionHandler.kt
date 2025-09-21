package com.sight.core.exception

import com.sight.controllers.http.dto.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BaseException::class)
    fun handleDiscordIntegrationNotFound(e: BaseException): ResponseEntity<ErrorResponse> {
        val errorResponse =
            ErrorResponse(
                statusCode = e.statusCode.value(),
                message = e.message,
                data = e.data,
                timestamp = Instant.now().toEpochMilli(),
            )
        return ResponseEntity.status(e.statusCode).body(errorResponse)
    }
}
