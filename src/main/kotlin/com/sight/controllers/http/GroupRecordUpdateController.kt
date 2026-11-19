package com.sight.controllers.http

import com.sight.controllers.http.dto.RecordFileUploadResponse
import com.sight.controllers.http.dto.UpdateGroupRecordRequest
import com.sight.controllers.http.dto.UpdateGroupRecordResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordUpdateService
import com.sight.service.RecordFileInput
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordUpdateController(private val service: GroupRecordUpdateService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.RECORD_AUTHOR], groupId = "groupId", recordId = "recordId", allowManager = false)
    @PatchMapping("/groups/{groupId}/records/{recordId}")
    @ResponseStatus(HttpStatus.OK)
    fun updateGroupRecord(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        @RequestBody request: UpdateGroupRecordRequest,
        requester: Requester,
    ): UpdateGroupRecordResponse {
        val uploads =
            service.updateGroupRecord(
                groupId,
                recordId,
                requester.userId,
                request.content,
                request.files.map { RecordFileInput(it.id, it.fileId, it.name, it.size, it.contentType) },
            )
        return UpdateGroupRecordResponse(uploads.map { RecordFileUploadResponse(it.id, it.fileId, it.url, it.uploadUrl) })
    }
}
