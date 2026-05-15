package com.sight.controllers.http.dto

import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategoryOld

data class ListSchedulesResponse(
    val count: Int,
    val schedules: List<ScheduleDto>,
) {
    companion object {
        fun from(schedules: List<Schedule>): ListSchedulesResponse {
            return ListSchedulesResponse(
                count = schedules.size,
                schedules = schedules.map { ScheduleDto.from(it) },
            )
        }
    }
}

data class ScheduleDto(
    val id: String,
    val title: String,
    val startTime: String,
    val category: ScheduleCategoryOld?,
) {
    companion object {
        fun from(schedule: Schedule): ScheduleDto {
            return ScheduleDto(
                id = schedule.id.toString(),
                title = schedule.title,
                startTime = schedule.scheduledAt.toString(),
                category = ScheduleCategoryOld.fromCode(schedule.categoryCode),
            )
        }
    }
}
