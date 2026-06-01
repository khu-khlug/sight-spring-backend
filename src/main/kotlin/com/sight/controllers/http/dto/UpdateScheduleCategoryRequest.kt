package com.sight.controllers.http.dto

import com.sight.domain.schedule.ScheduleCategory
import jakarta.validation.constraints.NotNull

data class UpdateScheduleCategoryRequest(
    @field:NotNull
    val category: ScheduleCategory,
    val isSummerSeason: Boolean?,
    val isSpeakAfter: Boolean?,
)
