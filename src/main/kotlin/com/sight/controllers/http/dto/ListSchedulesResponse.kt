package com.sight.controllers.http.dto

import com.sight.domain.schedule.Schedule
import com.sight.domain.schedule.ScheduleCategory

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
    val id: Long,
    val title: String,
    val category: ScheduleCategory,
    val location: String?,
    val state: String,
    val scheduledAt: String,
    val endAt: String,
    val expoint: Int,
    val author: Long,
    val groupId: Long?,
) {
    companion object {
        fun from(schedule: Schedule): ScheduleDto {
            return ScheduleDto(
                id = schedule.id,
                title = schedule.title,
                category = schedule.category,
                location = schedule.location,
                state = schedule.state.state,
                scheduledAt = schedule.scheduledAt.toString(),
                endAt = schedule.endAt.toString(),
                expoint = schedule.expoint,
                author = schedule.author,
                groupId = schedule.groupId,
            )
        }
    }
}
