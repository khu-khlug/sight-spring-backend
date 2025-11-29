package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateGroupMatchingFieldRequestRequest
import com.sight.controllers.http.dto.CreateGroupMatchingFieldRequestResponse
import com.sight.controllers.http.dto.GetFieldRequestsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.GroupMatchingFieldRequestService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class GroupMatchingFieldRequestController(
    private val groupMatchingFieldRequestService: GroupMatchingFieldRequestService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/field-requests")
    fun getFieldRequests(): List<GetFieldRequestsResponse> {
        return groupMatchingFieldRequestService.getAllFieldRequests()
    }

    @Auth([UserRole.USER])
    @PostMapping("/field-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroupMatchingFieldRequest(
        @Valid @RequestBody request: CreateGroupMatchingFieldRequestRequest,
        userId: Long,
    ): CreateGroupMatchingFieldRequestResponse {
        val saved =
            groupMatchingFieldRequestService.createGroupMatchingFieldRequest(
                request = request,
                requesterUserId = userId,
            )

        return CreateGroupMatchingFieldRequestResponse(
            id = saved.id,
            fieldName = saved.fieldName,
            requestReason = saved.requestReason,
            createdAt = saved.createdAt,
        )
    }
}
