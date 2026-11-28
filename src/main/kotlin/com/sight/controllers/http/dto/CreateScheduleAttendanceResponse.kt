package com.sight.controllers.http.dto

import com.sight.service.dto.CheckScheduleAttendanceResult
import java.time.LocalDateTime

data class CreateScheduleAttendanceResponse(
    val scheduleId: String,
    val userId: Long,
    val expointGranted: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(result: CheckScheduleAttendanceResult): CreateScheduleAttendanceResponse {
            return CreateScheduleAttendanceResponse(
                scheduleId = result.scheduleId.toString(),
                userId = result.userId,
                expointGranted = result.expointGranted,
                createdAt = result.createdAt,
            )
        }
    }
}
