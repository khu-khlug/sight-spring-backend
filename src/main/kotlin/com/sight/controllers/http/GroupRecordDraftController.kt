package com.sight.controllers.http

import com.sight.controllers.http.dto.DraftRecordFileUploadResponse
import com.sight.controllers.http.dto.GetGroupRecordDraftMetadataResponse
import com.sight.controllers.http.dto.GetGroupRecordDraftResponse
import com.sight.controllers.http.dto.GroupRecordDraftFileResponse
import com.sight.controllers.http.dto.GroupRecordDraftMetadataResponse
import com.sight.controllers.http.dto.GroupRecordDraftResponse
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordDraftController(
    private val groupRecordDraftService: GroupRecordDraftService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/records/draft/meta")
    @ResponseStatus(HttpStatus.OK)
    fun getGroupRecordDraftMetadata(
        @PathVariable @Positive groupId: Long,
        requester: Requester,
    ): GetGroupRecordDraftMetadataResponse {
        val result = groupRecordDraftService.getGroupRecordDraftMetadataByMemberId(groupId, requester.userId)
        return GetGroupRecordDraftMetadataResponse(
            result?.let {
                GroupRecordDraftMetadataResponse(it.isNewRecord, it.cardId.toString(), it.cardTitle, it.recordId?.toString(), it.updatedAt)
            },
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/records/draft")
    @ResponseStatus(HttpStatus.OK)
    fun getGroupRecordDraft(
        @PathVariable @Positive groupId: Long,
        requester: Requester,
    ): GetGroupRecordDraftResponse {
        val result = groupRecordDraftService.getGroupRecordDraftByMemberId(groupId, requester.userId)
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
            groupRecordDraftService.updateGroupRecordDraft(
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

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @DeleteMapping("/groups/{groupId}/records/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGroupRecordDraft(
        @PathVariable @Positive groupId: Long,
        requester: Requester,
    ) {
        groupRecordDraftService.deleteGroupRecordDraft(groupId, requester.userId)
    }
}
