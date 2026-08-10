package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateSmsMessageRequest
import com.sight.controllers.http.dto.CreateSmsMessageResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.SmsMessageService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SmsMessageController(
    private val smsMessageService: SmsMessageService,
) {
    @Auth([UserRole.MANAGER])
    @PostMapping("/manager/sms-messages")
    fun createSmsMessages(
        @Valid @RequestBody request: CreateSmsMessageRequest,
    ): ResponseEntity<CreateSmsMessageResponse> {
        val result =
            smsMessageService.createSmsMessages(
                memberIds = checkNotNull(request.memberIds).filterNotNull(),
                additionalPhoneNumbers = checkNotNull(request.additionalPhoneNumbers).filterNotNull(),
                message = checkNotNull(request.message),
            )
        val status = if (result.hasFailures) HttpStatus.UNPROCESSABLE_ENTITY else HttpStatus.OK
        return ResponseEntity.status(status).body(CreateSmsMessageResponse.from(result))
    }
}
