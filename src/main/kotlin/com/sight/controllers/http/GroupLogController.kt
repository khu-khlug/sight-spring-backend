package com.sight.controllers.http

import com.sight.controllers.http.dto.ListGroupLogActorResponse
import com.sight.controllers.http.dto.ListGroupLogResponse
import com.sight.controllers.http.dto.ListGroupLogsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupLogService
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class GroupLogController(
    private val groupLogService: GroupLogService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/groups/{groupId}/logs")
    fun listGroupLogs(
        @PathVariable groupId: Long,
        @RequestParam(defaultValue = "0") @Min(0) skip: Int,
        requester: Requester,
    ): ListGroupLogsResponse {
        val result =
            groupLogService.listGroupLogs(
                groupId = groupId,
                requesterId = requester.userId,
                skip = skip,
            )

        return ListGroupLogsResponse(
            skip = result.skip,
            totalCount = result.totalCount,
            receivedCount = result.receivedCount,
            logs =
                result.logs.map { log ->
                    ListGroupLogResponse(
                        id = log.id,
                        actor = log.actor?.let { ListGroupLogActorResponse(userId = it.userId, name = it.name) },
                        logType = log.logType.name,
                        logData = log.logData,
                        createdAt = log.createdAt,
                    )
                },
        )
    }
}
