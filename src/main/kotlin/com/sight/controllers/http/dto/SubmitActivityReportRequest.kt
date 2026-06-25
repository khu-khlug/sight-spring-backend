package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotNull

data class SubmitActivityReportRequest(
    @field:NotNull val isPresentation: Boolean?,
    @field:NotNull val fileUploadId: String?,
)
