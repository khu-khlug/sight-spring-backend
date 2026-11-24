package com.sight.repository.dto

import com.sight.domain.group.GroupActivityReport

data class GroupActivityReportFileView(
    val report: GroupActivityReport,
    val referenceId: String?,
    val fileUploadId: String?,
    val fileKey: String?,
)
