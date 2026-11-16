package com.sight.controllers.http

import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordDraftDeleteService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordDraftDeleteController(private val service: GroupRecordDraftDeleteService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @DeleteMapping("/groups/{groupId}/records/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGroupRecordDraft(
        @PathVariable groupId: Long,
        requester: Requester,
    ) {
        service.deleteGroupRecordDraft(groupId, requester.userId)
    }
}
