package com.sight.controllers.http.dto

import com.sight.domain.application.ApplicationFormStatus
import java.time.LocalDateTime

data class GetApplicationFormDetailResponse(
    val id: String,
    val submittee: String,
    val status: ApplicationFormStatus,
    val assignedUserId: Long?,
    val contents: List<Content>,
    val interviewAvailableTimes: List<Time>,
    val comments: List<Comment>,
) {
    data class Content(val questionId: String, val content: String)

    data class Time(val availableAt: String)

    data class Comment(val id: String, val authorUserId: Long, val content: String, val createdAt: LocalDateTime)
}
