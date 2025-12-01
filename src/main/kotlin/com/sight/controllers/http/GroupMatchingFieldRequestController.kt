package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateGroupMatchingFieldRequestRequest
import com.sight.controllers.http.dto.CreateGroupMatchingFieldRequestResponse
import com.sight.controllers.http.dto.GetFieldRequestsResponse
import com.sight.controllers.http.dto.RejectGroupMatchingFieldRequestRequest
import com.sight.controllers.http.dto.RejectGroupMatchingFieldRequestResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupMatchingFieldRequestService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/field-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroupMatchingFieldRequest(
        @Valid @RequestBody request: CreateGroupMatchingFieldRequestRequest,
        requester: Requester,
    ): CreateGroupMatchingFieldRequestResponse {
        val saved =
            groupMatchingFieldRequestService.createGroupMatchingFieldRequest(
                fieldName = request.fieldName,
                requestReason = request.requestReason,
                requesterUserId = requester.userId,
            )

        return CreateGroupMatchingFieldRequestResponse(
            id = saved.id,
            fieldName = saved.fieldName,
            requestReason = saved.requestReason,
            createdAt = saved.createdAt,
        )
    }

    @Auth([UserRole.MANAGER])
    @PostMapping("/field-requests/{fieldRequestId}/reject")
    fun rejectGroupMatchingFieldRequest(
        @PathVariable fieldRequestId: String,
        @Valid @RequestBody request: RejectGroupMatchingFieldRequestRequest,
    ): RejectGroupMatchingFieldRequestResponse {
        val rejected =
            groupMatchingFieldRequestService.rejectGroupMatchingFieldRequest(
                id = fieldRequestId,
                rejectReason = request.rejectReason,
            )

        return RejectGroupMatchingFieldRequestResponse(
            id = rejected.id,
            requesterUserId = rejected.requesterUserId,
            fieldName = rejected.fieldName,
            requestReason = rejected.requestReason,
            approvedAt = rejected.approvedAt,
            rejectedAt = rejected.rejectedAt,
            rejectReason = rejected.rejectReason,
            createdAt = rejected.createdAt,
        )
    }
}
