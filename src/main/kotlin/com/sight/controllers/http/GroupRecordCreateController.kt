package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateGroupRecordRequest
import com.sight.controllers.http.dto.CreateGroupRecordResponse
import com.sight.controllers.http.dto.CreatedRecordFileResponse
import com.sight.controllers.http.dto.RecordFileUploadResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordCreateService
import com.sight.service.RecordFileInput
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordCreateController(private val service: GroupRecordCreateService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PostMapping("/groups/{groupId}/cards/{cardId}/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroupRecord(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive cardId: Long,
        @RequestBody request: CreateGroupRecordRequest,
        requester: Requester,
    ): CreateGroupRecordResponse {
        val result =
            service.createGroupRecord(
                groupId,
                cardId,
                requester.userId,
                request.content,
                request.files.map { RecordFileInput(it.id, it.fileId, it.name, it.size, it.contentType) },
            )
        return CreateGroupRecordResponse(
            result.id.toString(), "TIPTAP", result.authorName, result.content, result.createdAt, result.createdAt,
            false, true, false, result.files.map { CreatedRecordFileResponse(it.id, it.fileId, it.url, it.name, it.size, it.contentType) },
            result.uploads.map { RecordFileUploadResponse(it.id, it.fileId, it.url, it.uploadUrl) },
        )
    }
}
