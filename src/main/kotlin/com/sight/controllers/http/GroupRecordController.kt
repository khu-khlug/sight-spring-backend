package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateGroupRecordRequest
import com.sight.controllers.http.dto.CreateGroupRecordResponse
import com.sight.controllers.http.dto.CreatedRecordFileResponse
import com.sight.controllers.http.dto.GetGroupRecordResponse
import com.sight.controllers.http.dto.ListGroupRecordResponse
import com.sight.controllers.http.dto.ListGroupRecordsResponse
import com.sight.controllers.http.dto.RecordFileUploadResponse
import com.sight.controllers.http.dto.UpdateGroupRecordRequest
import com.sight.controllers.http.dto.UpdateGroupRecordResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordReadService
import com.sight.service.GroupRecordWriteService
import com.sight.service.RecordFileInput
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordController(
    private val groupRecordReadService: GroupRecordReadService,
    private val groupRecordWriteService: GroupRecordWriteService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.VIEWER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/cards/{cardId}/records")
    @ResponseStatus(HttpStatus.OK)
    fun listGroupRecords(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive cardId: Long,
        @RequestParam(defaultValue = "0") offset: String,
        @RequestParam(defaultValue = "10") limit: String,
        @RequestParam(defaultValue = "false") includeDeleted: String,
        requester: Requester,
    ): ListGroupRecordsResponse {
        val parsedOffset =
            offset.takeIf { it.matches(Regex("[0-9]+")) }?.toLongOrNull()
                ?: throw BadRequestException("offset은 0 이상의 정수여야 합니다.")
        val parsedLimit =
            limit.takeIf { it.matches(Regex("[0-9]+")) }?.toIntOrNull()?.takeIf { it in 1..100 }
                ?: throw BadRequestException("limit은 1~100의 정수여야 합니다.")
        val deleted = includeDeleted.toBooleanStrictOrNull() ?: throw BadRequestException("includeDeleted는 불리언이어야 합니다.")
        val result = groupRecordReadService.listGroupRecords(groupId, cardId, requester.userId, parsedOffset, parsedLimit, deleted)
        return ListGroupRecordsResponse(
            result.offset,
            result.totalCount,
            result.records.size,
            result.records.map(ListGroupRecordResponse::from),
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.VIEWER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/records/{recordId}")
    @ResponseStatus(HttpStatus.OK)
    fun getGroupRecord(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        requester: Requester,
    ): GetGroupRecordResponse = GetGroupRecordResponse.from(groupRecordReadService.getGroupRecordById(groupId, recordId, requester.userId))

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
            groupRecordWriteService.createGroupRecord(
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
            groupRecordWriteService.updateGroupRecord(
                groupId,
                recordId,
                requester.userId,
                request.content,
                request.files.map { RecordFileInput(it.id, it.fileId, it.name, it.size, it.contentType) },
            )
        return UpdateGroupRecordResponse(uploads.map { RecordFileUploadResponse(it.id, it.fileId, it.url, it.uploadUrl) })
    }
}
