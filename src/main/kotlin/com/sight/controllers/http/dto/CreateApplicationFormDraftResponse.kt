package com.sight.controllers.http.dto

import com.sight.domain.application.ApplicationFormStatus
import java.time.LocalDateTime

data class CreateApplicationFormDraftResponse(
    val id: String,
    val info21Id: String,
    val submittee: String,
    val token: String,
    val status: ApplicationFormStatus,
    val interviewAvailableTimes: List<InterviewAvailableTimeResponse>,
    val contents: List<ApplicationContentResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    data class InterviewAvailableTimeResponse(
        val id: String,
        val availableAt: String,
        val createdAt: LocalDateTime,
    )

    data class ApplicationContentResponse(
        val id: String,
        val questionId: String,
        val content: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    )
}
