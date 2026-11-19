package com.sight.repository.dto

import com.sight.domain.group.GroupActivityReport
import java.time.LocalDateTime

data class GroupActivityReportView(
    val report: GroupActivityReport,
    val fileUploadId: String?,
    val fileKey: String?,
    val seminarScheduledAt: LocalDateTime?,
    val seminarIsSummerSeason: Boolean?,
    val seminarIsSpeakAfter: Boolean?,
)
