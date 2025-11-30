package com.sight.controllers.http

import com.sight.controllers.http.dto.ApproveFieldRequestResponse
import com.sight.controllers.http.dto.GetFieldRequestsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.GroupMatchingFieldRequestService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupMatchingFieldRequestController(
    private val groupMatchingFieldRequestService: GroupMatchingFieldRequestService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/field-requests")
    fun getFieldRequests(): List<GetFieldRequestsResponse> {
        return groupMatchingFieldRequestService.getAllFieldRequests()
    }

    @Auth(roles = [UserRole.MANAGER])
    @PostMapping("/field-requests/{fieldRequestId}/approve")
    @ResponseStatus(HttpStatus.CREATED)
    fun approveFieldRequest(
        @PathVariable fieldRequestId: String,
    ): ApproveFieldRequestResponse {
        return groupMatchingFieldRequestService.approveFieldRequest(fieldRequestId)
    }
}
