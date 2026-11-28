package com.sight.controllers.http.dto

import com.sight.service.ActivityReportListItem
import com.sight.service.ActivityReportListResult
import java.time.Instant

data class ListActivityReportsResponse(
    val reports: List<ListActivityReportResponse>,
) {
    companion object {
        fun from(result: ActivityReportListResult): ListActivityReportsResponse =
            ListActivityReportsResponse(result.reports.map(ListActivityReportResponse::from))
    }
}

data class ListActivityReportResponse(
    val id: String,
    val groupId: String,
    val seminarId: String,
    val seminarDate: Instant?,
    val seminarIsSummerSeason: Boolean?,
    val seminarIsSpeakAfter: Boolean?,
    val isPresentation: Boolean,
    val reportFileUrl: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(report: ActivityReportListItem): ListActivityReportResponse =
            ListActivityReportResponse(
                id = report.id,
                groupId = report.groupId.toString(),
                seminarId = report.seminarId,
                seminarDate = report.seminarDate,
                seminarIsSummerSeason = report.seminarIsSummerSeason,
                seminarIsSpeakAfter = report.seminarIsSpeakAfter,
                isPresentation = report.isPresentation,
                reportFileUrl = report.reportFileUrl,
                createdAt = report.createdAt,
                updatedAt = report.updatedAt,
            )
    }
}
