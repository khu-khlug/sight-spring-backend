package com.sight.controllers.http

import com.sight.controllers.http.dto.ListGroupRecordResponse
import com.sight.controllers.http.dto.ListGroupRecordsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupRecordReadService
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupRecordListController(private val service: GroupRecordReadService) {
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
        val result = service.listGroupRecords(groupId, cardId, requester.userId, parsedOffset, parsedLimit, deleted)
        return ListGroupRecordsResponse(
            result.offset,
            result.totalCount,
            result.records.size,
            result.records.map(ListGroupRecordResponse::from),
        )
    }
}
