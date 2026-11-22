package com.sight.controllers.http

import com.sight.controllers.http.dto.UpdateGroupRecordPortfolioRequest
import com.sight.controllers.http.dto.UpdateGroupRecordPositionRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordStateService
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordStateController(
    private val groupRecordStateService: GroupRecordStateService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @DeleteMapping("/groups/{groupId}/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGroupRecord(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        requester: Requester,
    ) {
        groupRecordStateService.deleteGroupRecord(groupId, requester.userId, recordId)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PostMapping("/groups/{groupId}/records/{recordId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun restoreGroupRecord(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        requester: Requester,
    ) {
        groupRecordStateService.updateGroupRecordRestored(groupId, requester.userId, recordId)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PutMapping("/groups/{groupId}/records/{recordId}/position")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateGroupRecordPosition(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        @RequestBody request: UpdateGroupRecordPositionRequest,
        requester: Requester,
    ) {
        groupRecordStateService.updateGroupRecordPosition(groupId, requester.userId, recordId, request.targetCardId.toLong())
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PutMapping("/groups/{groupId}/records/{recordId}/portfolio")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateGroupRecordPortfolio(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        @RequestBody request: UpdateGroupRecordPortfolioRequest,
        requester: Requester,
    ) {
        groupRecordStateService.updateGroupRecordPortfolio(groupId, requester.userId, recordId, request.isInPortfolio)
    }
}
