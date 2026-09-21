package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotEmpty

data class CreateScheduleAttendanceListRequest(
    @field:NotEmpty(message = "출석 체크할 유저를 최소 1명 이상 선택해 주세요.")
    val userIds: List<Long>,
)
