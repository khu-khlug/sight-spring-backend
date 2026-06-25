package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateScheduleRequest
import com.sight.controllers.http.dto.CreateScheduleResponse
import com.sight.controllers.http.dto.GetScheduleResponse
import com.sight.controllers.http.dto.ListSchedulesResponse
import com.sight.controllers.http.dto.UpdateScheduleCategoryRequest
import com.sight.controllers.http.dto.UpdateScheduleRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.ScheduleService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
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
        @RequestParam(defaultValue = "50")
        @Min(1)
        @Max(50)
        limit: Int,
    ): ListSchedulesResponse {
        val schedules = scheduleService.listSchedules(from, limit)
        return ListSchedulesResponse.from(schedules)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/schedules/{scheduleId}")
    fun getSchedule(
        requester: Requester,
        @PathVariable scheduleId: Long,
    ): GetScheduleResponse {
        val (schedule, authorName, groupTitle) = scheduleService.getScheduleWithDetails(scheduleId)
        return GetScheduleResponse.from(schedule, requester.role, authorName = authorName, groupTitle = groupTitle)
    }

    @Auth([UserRole.MANAGER])
    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSchedule(
        requester: Requester,
        @Valid @RequestBody request: CreateScheduleRequest,
    ): CreateScheduleResponse {
        val schedule =
            scheduleService.createSchedule(
                requester = requester,
                title = request.title,
                category = request.category,
                location = request.location,
                scheduledAt = request.scheduledAt,
                endAt = request.endAt,
                expoint = request.expoint,
                generateCheckCode = request.generateCheckCode,
            )
        return CreateScheduleResponse.from(schedule)
    }

    @Auth([UserRole.MANAGER])
    @PatchMapping("/schedules/{scheduleId}")
    fun updateSchedule(
        requester: Requester,
        @PathVariable scheduleId: Long,
        @Valid @RequestBody request: UpdateScheduleRequest,
    ): GetScheduleResponse {
        val schedule =
            scheduleService.updateSchedule(
                requester = requester,
                id = scheduleId,
                title = request.title,
                location = request.location,
                scheduledAt = request.scheduledAt,
                endAt = request.endAt,
                expoint = request.expoint,
            )
        return GetScheduleResponse.from(schedule, requester.role)
    }

    @Auth([UserRole.MANAGER])
    @PatchMapping("/schedules/{scheduleId}/category")
    fun updateScheduleCategory(
        requester: Requester,
        @PathVariable scheduleId: Long,
        @Valid @RequestBody request: UpdateScheduleCategoryRequest,
    ): GetScheduleResponse {
        val (schedule, bigSeminar) =
            scheduleService.updateScheduleCategory(
                requester = requester,
                id = scheduleId,
                category = request.category,
                isSummerSeason = request.isSummerSeason,
                isSpeakAfter = request.isSpeakAfter,
            )
        return GetScheduleResponse.from(schedule, requester.role, bigSeminar)
    }

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSchedule(
        requester: Requester,
        @PathVariable scheduleId: Long,
    ) {
        scheduleService.deleteSchedule(requester, scheduleId)
    }
}
