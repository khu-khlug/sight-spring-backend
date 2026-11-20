package com.sight.controllers.http.dto

import com.sight.core.id.IntegerId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateGroupActivityScheduleRequest(
    @field:NotBlank
    val title: String,
    val location: String?,
    @field:NotNull
    val scheduledAt: LocalDateTime,
    @field:NotNull
    val endAt: LocalDateTime,
    @field:NotNull
    @field:IntegerId
    val groupId: String,
)
