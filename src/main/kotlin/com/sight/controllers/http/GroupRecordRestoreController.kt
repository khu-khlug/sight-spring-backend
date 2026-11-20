package com.sight.controllers.http

import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordStateService
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordRestoreController(private val service: GroupRecordStateService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PostMapping("/groups/{groupId}/records/{recordId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun restoreGroupRecord(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        requester: Requester,
    ) {
        service.updateGroupRecordRestored(groupId, requester.userId, recordId)
    }
}
