package com.sight.controllers.http.dto

import com.sight.service.dto.ListScheduleAttendancesResult
import java.time.LocalDateTime

data class ListScheduleAttendanceResponse(
    val userId: Long,
    val isChecked: Boolean,
    val createdAt: LocalDateTime,
)

data class ListScheduleAttendancesResponse(
    val count: Int,
    val attendances: List<ListScheduleAttendanceResponse>,
) {
    companion object {
        fun from(result: ListScheduleAttendancesResult): ListScheduleAttendancesResponse {
            val attendances =
                result.attendances.map { item ->
                    ListScheduleAttendanceResponse(
                        userId = item.userId,
                        isChecked = item.isChecked,
                        createdAt = item.createdAt,
                    )
                }
            return ListScheduleAttendancesResponse(
                count = result.count,
                attendances = attendances,
            )
        }
    }
}
