package com.sight.controllers.http

import com.sight.controllers.http.dto.GetGroupRecordDraftResponse
import com.sight.controllers.http.dto.GroupRecordDraftFileResponse
import com.sight.controllers.http.dto.GroupRecordDraftResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordDraftReadService
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordDraftReadController(private val service: GroupRecordDraftReadService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/records/draft")
    @ResponseStatus(HttpStatus.OK)
    fun getGroupRecordDraft(
        @PathVariable @Positive groupId: Long,
        requester: Requester,
    ): GetGroupRecordDraftResponse {
        val result = service.getGroupRecordDraftByMemberId(groupId, requester.userId)
        return GetGroupRecordDraftResponse(
            GroupRecordDraftResponse(
                result.isNewRecord,
                result.cardId.toString(),
                result.cardTitle,
                result.recordId?.toString(),
                result.content,
                result.updatedAt,
                result.files.map { GroupRecordDraftFileResponse(it.id, it.fileId, it.url, it.name, it.size, it.contentType) },
            ),
        )
    }
}
