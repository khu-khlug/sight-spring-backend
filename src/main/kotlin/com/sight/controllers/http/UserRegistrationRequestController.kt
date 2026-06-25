package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateUserRegistrationRequest
import com.sight.controllers.http.dto.CreateUserRegistrationResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.UserRegistrationRequestService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class UserRegistrationRequestController(
    private val userRegistrationRequestService: UserRegistrationRequestService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/user-registration-requests")
    fun createRegistrationRequest(
        requester: Requester,
        @Valid @RequestBody request: CreateUserRegistrationRequest,
    ): CreateUserRegistrationResponse {
        val registrationRequest =
            userRegistrationRequestService.createRegistrationRequest(
                info21Id = request.info21Id,
                info21Password = request.info21Password,
                requestedUserId = requester.userId,
            )

        return CreateUserRegistrationResponse(
            id = registrationRequest.id,
            requestedUserId = registrationRequest.requestedUserId,
            status = registrationRequest.status.name,
            createdAt = registrationRequest.createdAt,
            updatedAt = registrationRequest.updatedAt,
        )
    }
}
