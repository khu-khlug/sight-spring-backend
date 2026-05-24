package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateScheduleAttendanceRequest
import com.sight.controllers.http.dto.CreateScheduleAttendanceResponse
import com.sight.controllers.http.dto.ListScheduleAttendancesResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.ScheduleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ScheduleAttendanceController(
    private val scheduleService: ScheduleService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/schedules/{scheduleId}/attendances")
    fun listScheduleAttendances(
        @PathVariable scheduleId: Long,
    ): ListScheduleAttendancesResponse {
        val result = scheduleService.listScheduleAttendances(scheduleId)
        return ListScheduleAttendancesResponse.from(result)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/schedules/{scheduleId}/attendances/@me")
    @ResponseStatus(HttpStatus.CREATED)
    fun checkScheduleAttendance(
        requester: Requester,
        @PathVariable scheduleId: Long,
        @Valid @RequestBody request: CreateScheduleAttendanceRequest,
    ): CreateScheduleAttendanceResponse {
        val result =
            scheduleService.checkScheduleAttendance(
                requester = requester,
                scheduleId = scheduleId,
                code = request.code,
            )
        return CreateScheduleAttendanceResponse.from(result)
    }

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/schedules/{scheduleId}/attendances/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeScheduleAttendance(
        requester: Requester,
        @PathVariable scheduleId: Long,
        @PathVariable userId: Long,
    ) {
        scheduleService.removeScheduleAttendance(
            requester = requester,
            scheduleId = scheduleId,
            userId = userId,
        )
    }
}
