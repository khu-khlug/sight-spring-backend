package com.sight.controllers.http.dto

import com.sight.domain.schedule.Schedule

data class ListAttendanceHistoryResponse(
    val count: Int,
    val schedules: List<ScheduleDto>,
) {
    companion object {
        fun from(schedules: List<Schedule>): ListAttendanceHistoryResponse {
            return ListAttendanceHistoryResponse(
                count = schedules.size,
                schedules = schedules.map { ScheduleDto.from(it) },
            )
        }
    }
}
