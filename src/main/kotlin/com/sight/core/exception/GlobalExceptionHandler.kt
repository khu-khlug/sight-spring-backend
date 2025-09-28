package com.sight.core.exception

import com.sight.controllers.http.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors =
            e.bindingResult.fieldErrors.map { fieldError ->
                "${fieldError.field}: ${fieldError.defaultMessage}"
            }

        val errorResponse =
            ErrorResponse(
                statusCode = HttpStatus.BAD_REQUEST.value(),
                message = "입력값이 올바르지 않습니다",
                data = mapOf("errors" to errors),
                timestamp = Instant.now().toEpochMilli(),
            )
        return ResponseEntity.badRequest().body(errorResponse)
    }
}
