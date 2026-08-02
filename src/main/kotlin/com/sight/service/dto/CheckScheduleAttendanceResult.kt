package com.sight.service.dto

import java.time.LocalDateTime

data class CheckScheduleAttendanceResult(
    val scheduleId: Long,
    val userId: Long,
    val expointGranted: Int,
    val createdAt: LocalDateTime,
)
