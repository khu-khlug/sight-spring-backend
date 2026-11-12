package com.sight.controllers.http

import com.sight.controllers.http.dto.ListSavedRecordResponse
import com.sight.controllers.http.dto.ListSavedRecordsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupRecordArchiveService
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class SavedRecordController(
    private val groupRecordArchiveService: GroupRecordArchiveService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/saved-records")
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
