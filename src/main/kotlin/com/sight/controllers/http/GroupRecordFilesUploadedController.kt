package com.sight.controllers.http

import com.sight.controllers.http.dto.GroupRecordFileVerificationResponse
import com.sight.controllers.http.dto.UpdateGroupRecordFilesUploadedRequest
import com.sight.controllers.http.dto.UpdateGroupRecordFilesUploadedResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordFilesUploadedService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordFilesUploadedController(private val service: GroupRecordFilesUploadedService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PostMapping("/groups/{groupId}/records/files/uploaded")
    fun updateGroupRecordFilesUploaded(
        @PathVariable groupId: Long,
        @RequestBody request: UpdateGroupRecordFilesUploadedRequest,
        requester: Requester,
    ): ResponseEntity<UpdateGroupRecordFilesUploadedResponse> {
        val results = service.updateGroupRecordFilesUploaded(groupId, requester.userId, request.fileIds)
        val status =
            when {
                results.all { it.isSuccess } -> HttpStatus.OK
                results.any { it.isSuccess } -> HttpStatus.PARTIAL_CONTENT
                else -> HttpStatus.BAD_REQUEST
            }
        return ResponseEntity.status(status).body(
            UpdateGroupRecordFilesUploadedResponse(
                results.map { GroupRecordFileVerificationResponse(it.fileId, it.isSuccess, it.reason) },
            ),
        )
    }
}
