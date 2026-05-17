package com.sight.controllers.http.dto

import com.sight.domain.schedule.ScheduleCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDateTime

data class UpdateScheduleRequest(
    @field:NotBlank
    val title: String,
    @field:NotNull
    val category: ScheduleCategory,
    val location: String?,
    @field:NotNull
    val startTime: LocalDateTime,
    @field:NotNull
    val endTime: LocalDateTime,
    @field:PositiveOrZero
    val expoint: Int,
)
