package com.sight.controllers.http

import com.sight.controllers.http.dto.GetGroupRecordResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordReadService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordReadController(private val service: GroupRecordReadService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.VIEWER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/records/{recordId}")
    @ResponseStatus(HttpStatus.OK)
    fun getGroupRecord(
        @PathVariable groupId: Long,
        @PathVariable recordId: Long,
        requester: Requester,
    ): GetGroupRecordResponse = GetGroupRecordResponse.from(service.getGroupRecordById(groupId, recordId, requester.userId))
}
