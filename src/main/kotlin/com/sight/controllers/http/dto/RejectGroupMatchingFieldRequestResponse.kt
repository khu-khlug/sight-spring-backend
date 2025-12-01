package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class RejectGroupMatchingFieldRequestResponse(
    val id: String,
    val requesterUserId: Long,
    val fieldName: String,
    val requestReason: String,
    val approvedAt: LocalDateTime?,
    val rejectedAt: LocalDateTime?,
    val rejectReason: String?,
    val createdAt: LocalDateTime,
)
