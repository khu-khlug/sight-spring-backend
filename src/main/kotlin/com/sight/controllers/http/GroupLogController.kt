package com.sight.controllers.http

import com.sight.controllers.http.dto.ListGroupLogResponse
import com.sight.controllers.http.dto.ListGroupLogsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupLogService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupLogController(
    private val groupLogService: GroupLogService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/groups/{groupId}/logs")
    fun listGroupLogs(
        @PathVariable groupId: Long,
        @RequestParam(defaultValue = "0") @Min(0) offset: Int,
        @RequestParam(defaultValue = "100") @Min(1) @Max(100) limit: Int,
        requester: Requester,
    ): ListGroupLogsResponse {
        val result =
            groupLogService.listGroupLogs(
                groupId = groupId,
                requesterId = requester.userId,
                offset = offset,
                limit = limit,
            )

        return ListGroupLogsResponse(
            count = result.count,
            logs =
                result.logs.map { log ->
                    ListGroupLogResponse(
                        id = log.id,
                        memberId = log.memberId,
                        message = log.message,
                        createdAt = log.createdAt,
                    )
                },
        )
    }
}
