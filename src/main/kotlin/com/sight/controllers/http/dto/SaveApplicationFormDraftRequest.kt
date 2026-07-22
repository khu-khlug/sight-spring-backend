package com.sight.controllers.http.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class SaveApplicationFormDraftRequest(
    @field:NotBlank val token: String,
    @field:NotEmpty @field:Valid val interviewAvailableTimes: List<InterviewAvailableTimeRequest>,
    @field:NotEmpty @field:Valid val contents: List<ContentRequest>,
) {
    data class InterviewAvailableTimeRequest(
        @field:NotBlank val date: String,
        @field:NotBlank val time: String,
    )

    data class ContentRequest(
        @field:NotBlank val questionId: String,
        val content: String,
    )
}
