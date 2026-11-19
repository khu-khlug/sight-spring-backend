package com.sight.controllers.http.dto

import com.sight.service.ActivityReportResult
import java.time.Instant

data class ActivityReportResponse(
    val id: String,
    val groupId: String,
    val seminarId: String,
    val isPresentation: Boolean,
    val reportFileKey: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(report: ActivityReportResult): ActivityReportResponse =
            ActivityReportResponse(
                id = report.id,
                groupId = report.groupId.toString(),
                seminarId = report.seminarId,
                isPresentation = report.isPresentation,
                reportFileKey = report.reportFileKey,
                createdAt = report.createdAt,
                updatedAt = report.updatedAt,
            )
    }
}
