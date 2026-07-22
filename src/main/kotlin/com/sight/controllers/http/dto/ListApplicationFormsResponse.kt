package com.sight.controllers.http.dto

import com.sight.domain.application.ApplicationFormStatus
import java.time.LocalDateTime

data class ListApplicationFormsResponse(
    val applications: List<Application>,
    val count: Long,
) {
    data class Application(
        val id: String,
        val submittee: String,
        val status: ApplicationFormStatus,
        val assignedUserId: Long?,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    )
}
