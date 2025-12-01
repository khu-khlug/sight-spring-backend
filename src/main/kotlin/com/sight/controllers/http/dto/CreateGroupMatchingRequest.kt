package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateGroupMatchingRequest(
    @field:NotNull
    val year: Int,
    @field:NotNull
    val semester: Int,
    @field:NotNull
    val closedAt: LocalDateTime,
)
