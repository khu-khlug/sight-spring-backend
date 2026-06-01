package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateGroupActivityScheduleRequest
import com.sight.controllers.http.dto.CreateScheduleResponse
import com.sight.controllers.http.dto.GetScheduleResponse
import com.sight.controllers.http.dto.UpdateGroupActivityScheduleRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.ScheduleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupActivityScheduleController(
    private val scheduleService: ScheduleService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/schedules/group-activity")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroupActivitySchedule(
        requester: Requester,
        @Valid @RequestBody request: CreateGroupActivityScheduleRequest,
    ): CreateScheduleResponse {
        val schedule =
            scheduleService.createGroupActivitySchedule(
                requester = requester,
                title = request.title,
                location = request.location,
                scheduledAt = request.scheduledAt,
                endAt = request.endAt,
            )
        return CreateScheduleResponse.from(schedule)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PatchMapping("/schedules/group-activity/{scheduleId}")
    fun updateGroupActivitySchedule(
        requester: Requester,
        @PathVariable scheduleId: Long,
        @Valid @RequestBody request: UpdateGroupActivityScheduleRequest,
    ): GetScheduleResponse {
        val schedule =
            scheduleService.updateGroupActivitySchedule(
                requester = requester,
                id = scheduleId,
                title = request.title,
                location = request.location,
                scheduledAt = request.scheduledAt,
                endAt = request.endAt,
            )
        return GetScheduleResponse.from(schedule, requester.role)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @DeleteMapping("/schedules/group-activity/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGroupActivitySchedule(
        requester: Requester,
        @PathVariable scheduleId: Long,
    ) {
        scheduleService.deleteGroupActivitySchedule(requester, scheduleId)
    }
}
