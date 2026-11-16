package com.sight.controllers.http

import com.sight.controllers.http.dto.UpdateGroupRecordPortfolioRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordPortfolioService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordPortfolioController(private val service: GroupRecordPortfolioService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PutMapping("/groups/{groupId}/records/{recordId}/portfolio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateGroupRecordPortfolio(
        @PathVariable groupId: Long,
        @PathVariable recordId: Long,
        @RequestBody request: UpdateGroupRecordPortfolioRequest,
        requester: Requester,
    ) {
        service.updateGroupRecordPortfolio(groupId, requester.userId, recordId, request.isInPortfolio)
    }
}
