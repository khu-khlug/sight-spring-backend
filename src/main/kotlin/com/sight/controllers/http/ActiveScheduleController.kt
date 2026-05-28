package com.sight.controllers.http

import com.sight.controllers.http.dto.ListActiveSchedulesResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.ScheduleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ActiveScheduleController(
    private val scheduleService: ScheduleService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER, UserRole.SYSTEM])
    @GetMapping("/active-schedules")
    fun listActiveSchedules(): ListActiveSchedulesResponse {
        val schedules = scheduleService.listActiveSchedules()
        return ListActiveSchedulesResponse.from(schedules)
    }
}
