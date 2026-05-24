package com.sight.controllers.http.dto

import com.sight.domain.schedule.Schedule

data class ListActiveSchedulesResponse(
    val count: Int,
    val schedules: List<ScheduleDto>,
    val schedule: ScheduleDto?,
) {
    companion object {
        fun from(schedules: List<Schedule>): ListActiveSchedulesResponse {
            val scheduleDtos = schedules.map { ScheduleDto.from(it) }
            return ListActiveSchedulesResponse(
                count = scheduleDtos.size,
                schedules = scheduleDtos,
                schedule = scheduleDtos.firstOrNull(),
            )
        }
    }
}
