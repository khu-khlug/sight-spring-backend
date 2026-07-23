package com.sight.service.dto

import java.time.LocalDateTime

data class ListScheduleAttendancesResult(
    val attendances: List<ScheduleAttendanceItem>,
) {
    val count: Int
        get() = attendances.size
}

data class ScheduleAttendanceItem(
    val userId: Long,
    val isChecked: Boolean,
    val createdAt: LocalDateTime,
)
