package com.sight.controllers.http

import com.sight.controllers.http.dto.ListScheduleAttendancesResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.ScheduleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
}
