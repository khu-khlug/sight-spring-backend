package com.sight.controllers.http

import com.sight.controllers.http.dto.UpdateUserRegistrationRequestStatusRequest
import com.sight.controllers.http.dto.UpdateUserRegistrationRequestStatusResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.domain.application.UserRegistrationRequestStatus
import com.sight.service.UserRegistrationRequestService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserRegistrationRequestController(
    private val userRegistrationRequestService: UserRegistrationRequestService,
) {
    @Auth([UserRole.MANAGER])
    @PutMapping("/manager/user-registration-requests/{requestId}")
    fun updateStatus(
        @PathVariable requestId: String,
        @Valid @RequestBody request: UpdateUserRegistrationRequestStatusRequest,
    ): UpdateUserRegistrationRequestStatusResponse {
        val status =
            UserRegistrationRequestStatus.entries.find {
                it.name.equals(request.status, ignoreCase = true)
            } ?: throw BadRequestException("지원하지 않는 회원 등록 요청 상태입니다")

        if (status != UserRegistrationRequestStatus.APPROVED) {
            throw BadRequestException("지원하지 않는 회원 등록 요청 상태입니다")
        }

        userRegistrationRequestService.approve(requestId)

        return UpdateUserRegistrationRequestStatusResponse(
            content = "승인되었습니다",
        )
    }
}
