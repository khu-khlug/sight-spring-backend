package com.sight.controllers.http

import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordDeleteService
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordDeleteController(private val service: GroupRecordDeleteService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @DeleteMapping("/groups/{groupId}/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGroupRecord(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        requester: Requester,
    ) {
        service.deleteGroupRecord(groupId, requester.userId, recordId)
    }
}
