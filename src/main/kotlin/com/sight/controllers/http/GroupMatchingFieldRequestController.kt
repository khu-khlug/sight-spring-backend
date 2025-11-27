package com.sight.controllers.http

import com.sight.controllers.http.dto.GetFieldRequestsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.GroupMatchingFieldRequestService
import org.springframework.web.bind.annotation.GetMapping
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
}
