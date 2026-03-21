package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class UpdateGroupMatchingClosedAtRequest(
    @field:NotNull(message = "마감일은 필수입니다")
    val closedAt: LocalDate,
)
