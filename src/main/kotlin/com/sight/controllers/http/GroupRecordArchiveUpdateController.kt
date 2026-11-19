package com.sight.controllers.http

import com.sight.controllers.http.dto.UpdateGroupRecordArchiveRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordArchiveUpdateService
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordArchiveUpdateController(private val service: GroupRecordArchiveUpdateService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PutMapping("/groups/{groupId}/records/{recordId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateGroupRecordArchive(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        @RequestBody request: UpdateGroupRecordArchiveRequest,
        requester: Requester,
    ) {
        service.updateGroupRecordArchive(groupId, requester.userId, recordId, request.isInUserArchive)
    }
}
