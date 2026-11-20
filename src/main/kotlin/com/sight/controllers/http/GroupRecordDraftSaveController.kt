package com.sight.controllers.http

import com.sight.controllers.http.dto.DraftRecordFileUploadResponse
import com.sight.controllers.http.dto.UpdateGroupRecordDraftRequest
import com.sight.controllers.http.dto.UpdateGroupRecordDraftResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordDraftService
import com.sight.service.RecordFileInput
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordDraftSaveController(private val service: GroupRecordDraftService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PutMapping("/groups/{groupId}/records/draft")
    @ResponseStatus(HttpStatus.OK)
    fun updateGroupRecordDraft(
        @PathVariable @Positive groupId: Long,
        @RequestBody request: UpdateGroupRecordDraftRequest,
        requester: Requester,
    ): UpdateGroupRecordDraftResponse {
        val uploads =
            service.updateGroupRecordDraft(
                groupId,
                requester.userId,
                request.cardId.toLong(),
                request.recordId?.toLong(),
                request.content,
                request.updatedAt,
                request.files.map { RecordFileInput(it.id, it.fileId, it.name, it.size, it.contentType) },
            )
        return UpdateGroupRecordDraftResponse(uploads.map { DraftRecordFileUploadResponse(it.id, it.fileId, it.url, it.uploadUrl) })
    }
}
