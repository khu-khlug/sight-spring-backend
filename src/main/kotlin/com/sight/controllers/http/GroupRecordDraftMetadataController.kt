package com.sight.controllers.http

import com.sight.controllers.http.dto.GetGroupRecordDraftMetadataResponse
import com.sight.controllers.http.dto.GroupRecordDraftMetadataResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordDraftMetadataService
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordDraftMetadataController(private val service: GroupRecordDraftMetadataService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/records/draft/meta")
    @ResponseStatus(HttpStatus.OK)
    fun getGroupRecordDraftMetadata(
        @PathVariable @Positive groupId: Long,
        requester: Requester,
    ): GetGroupRecordDraftMetadataResponse {
        val result = service.getGroupRecordDraftMetadataByMemberId(groupId, requester.userId)
        return GetGroupRecordDraftMetadataResponse(
            result?.let {
                GroupRecordDraftMetadataResponse(it.isNewRecord, it.cardId.toString(), it.cardTitle, it.recordId?.toString(), it.updatedAt)
            },
        )
    }
}
