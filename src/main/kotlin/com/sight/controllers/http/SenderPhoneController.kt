package com.sight.controllers.http

import com.sight.controllers.http.dto.GetSenderPhoneResponse
import com.sight.controllers.http.dto.UpdateSenderPhoneRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.SenderPhoneService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class SenderPhoneController(
    private val senderPhoneService: SenderPhoneService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/manager/sender-phone")
    fun getSenderPhone(): GetSenderPhoneResponse = GetSenderPhoneResponse(phone = senderPhoneService.getSenderPhone())

    @Auth([UserRole.MANAGER])
    @PutMapping("/manager/sender-phone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateSenderPhone(
        @Valid @RequestBody request: UpdateSenderPhoneRequest,
    ) {
        senderPhoneService.updateSenderPhone(checkNotNull(request.phone))
    }
}
