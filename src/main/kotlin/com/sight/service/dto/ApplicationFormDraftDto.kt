package com.sight.service.dto

import com.sight.domain.application.ApplicationFormStatus
import java.time.LocalDateTime

data class ApplicationFormDraftDto(
    val id: String,
    val info21Id: String,
    val submittee: String,
    val token: String,
    val status: ApplicationFormStatus,
    val interviewAvailableTimes: List<InterviewAvailableTimeDto>,
    val contents: List<ApplicationContentDto>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    data class InterviewAvailableTimeDto(
        val id: String,
        val availableAt: String,
        val createdAt: LocalDateTime,
    )

    data class ApplicationContentDto(
        val id: String,
        val questionId: String,
        val content: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    )
}
