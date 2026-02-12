package com.sight.controllers.http

import com.sight.controllers.http.dto.ListSchedulesResponse
import com.sight.service.ScheduleService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@Validated
class ScheduleController(
    private val scheduleService: ScheduleService,
) {
    @GetMapping("/schedules")
    fun listSchedules(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,
        @RequestParam(defaultValue = "5")
        @Min(1)
        @Max(50)
        limit: Int,
    ): ListSchedulesResponse {
        val fromDateTime = from ?: LocalDateTime.now()
        val schedules = scheduleService.listSchedules(fromDateTime, limit)
        return ListSchedulesResponse.from(schedules)
    }
}
