package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateBigSeminarScheduleRequest
import com.sight.controllers.http.dto.CreateScheduleResponse
import com.sight.controllers.http.dto.GetScheduleResponse
import com.sight.controllers.http.dto.UpdateBigSeminarScheduleRequest
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
class BigSeminarScheduleController(
    private val scheduleService: ScheduleService,
) {
    @Auth([UserRole.MANAGER])
    @PostMapping("/schedules/big-seminar")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBigSeminarSchedule(
        requester: Requester,
        @Valid @RequestBody request: CreateBigSeminarScheduleRequest,
    ): CreateScheduleResponse {
        val (schedule, bigSeminar) =
            scheduleService.createBigSeminarSchedule(
                requester = requester,
                title = request.title,
                location = request.location,
                scheduledAt = request.scheduledAt,
                endAt = request.endAt,
                expoint = request.expoint,
                generateCheckCode = request.generateCheckCode,
                isSummerSeason = request.isSummerSeason,
                isSpeakAfter = request.isSpeakAfter,
            )
        return CreateScheduleResponse.from(schedule, bigSeminar)
    }

    @Auth([UserRole.MANAGER])
    @PatchMapping("/schedules/big-seminar/{scheduleId}")
    fun updateBigSeminarSchedule(
        requester: Requester,
        @PathVariable scheduleId: Long,
        @Valid @RequestBody request: UpdateBigSeminarScheduleRequest,
    ): GetScheduleResponse {
        val (schedule, bigSeminar) =
            scheduleService.updateBigSeminarSchedule(
                requester = requester,
                id = scheduleId,
                title = request.title,
                location = request.location,
                scheduledAt = request.scheduledAt,
                endAt = request.endAt,
                expoint = request.expoint,
                isSummerSeason = request.isSummerSeason,
                isSpeakAfter = request.isSpeakAfter,
            )
        return GetScheduleResponse.from(schedule, requester.role, bigSeminar)
    }

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/schedules/big-seminar/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteBigSeminarSchedule(
        requester: Requester,
        @PathVariable scheduleId: Long,
    ) {
        scheduleService.deleteBigSeminarSchedule(requester, scheduleId)
    }
}
