package com.sight.controllers.http.dto

import jakarta.validation.constraints.Pattern

data class CreateScheduleAttendanceRequest(
    @field:Pattern(regexp = "\\d{4}", message = "출석 코드는 4자리 숫자여야 합니다")
    val code: String,
)
