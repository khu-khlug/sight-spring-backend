package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class GetFieldRequestsResponse(
    val id: String,
    val fieldName: String,
    val requestedBy: Long,
    val requestedAt: LocalDateTime,
    val requestReason: String,
    val status: FieldRequestStatus,
    val processDetails: ProcessDetails?,
)

enum class FieldRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
}

data class ProcessDetails(
    val processedAt: LocalDateTime,
    val rejectReason: String?,
)
