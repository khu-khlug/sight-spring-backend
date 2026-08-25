package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDateTime

data class UpdateBigSeminarScheduleRequest(
    @field:NotBlank
    val title: String,
    val location: String?,
    @field:NotNull
    val scheduledAt: LocalDateTime,
    @field:NotNull
    val endAt: LocalDateTime,
    @field:PositiveOrZero
    val expoint: Int,
    val isSummerSeason: Boolean,
    val isSpeakAfter: Boolean,
)
