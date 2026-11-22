package com.sight.controllers.http

import com.sight.controllers.http.dto.ListSavedRecordResponse
import com.sight.controllers.http.dto.ListSavedRecordsResponse
import com.sight.controllers.http.dto.UpdateGroupRecordArchiveRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordArchiveService
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordArchiveController(
    private val groupRecordArchiveService: GroupRecordArchiveService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = false)
    @PutMapping("/groups/{groupId}/records/{recordId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateGroupRecordArchive(
        @PathVariable @Positive groupId: Long,
        @PathVariable @Positive recordId: Long,
        @RequestBody request: UpdateGroupRecordArchiveRequest,
        requester: Requester,
    ) {
        groupRecordArchiveService.updateGroupRecordArchive(groupId, requester.userId, recordId, request.isInUserArchive)
    }
}

@Validated
@RestController
class SavedRecordController(
    private val groupRecordArchiveService: GroupRecordArchiveService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/saved-records")
    @ResponseStatus(HttpStatus.OK)
    fun listSavedRecords(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        requester: Requester,
    ): ListSavedRecordsResponse {
        val result =
            groupRecordArchiveService.listSavedRecords(
                memberId = requester.userId,
                page = page,
            )

        return ListSavedRecordsResponse(
            page = result.page,
            totalCount = result.totalCount,
            receivedCount = result.receivedCount,
            records =
                result.records.map { record ->
                    ListSavedRecordResponse(
                        recordId = record.recordId.toString(),
                        type = record.type.name,
                        content = record.content,
                        groupId = record.groupId.toString(),
                        groupName = record.groupName,
                        cardId = record.cardId.toString(),
                        cardTitle = record.cardTitle,
                        isDeleted = record.isDeleted,
                        isCardDeleted = record.isCardDeleted,
                        authorName = record.authorName,
                        createdAt = record.createdAt,
                        savedAt = record.savedAt,
                    )
                },
        )
    }
}
