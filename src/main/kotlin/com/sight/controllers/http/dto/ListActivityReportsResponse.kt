package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class ListActivityReportsResponse(
    val reports: List<ListActivityReportResponse>,
)

data class ListActivityReportResponse(
    val id: String,
    val groupId: Long,
    val seminarDate: LocalDateTime?,
    val seminarIsSummerSeason: Boolean?,
    val seminarIsSpeakAfter: Boolean?,
    val isPresentation: Boolean,
    val reportFileUrl: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
