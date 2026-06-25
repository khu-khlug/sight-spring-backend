package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class ActivityReportResponse(
    val id: String,
    val groupId: Long,
    val seminarId: String,
    val isPresentation: Boolean,
    val reportFileKey: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
