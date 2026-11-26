package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

data class SubmitActivityReportRequest(
    val isPresentation: Boolean,
    val fileUploadId: String,
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(body: JsonNode): SubmitActivityReportRequest {
            val fields = ActivityReportRequestFields.parse(body)
            val isPresentation = fields.isPresentation ?: throw BadRequestException("발표 여부가 필요합니다.")
            val fileUploadId = fields.fileUploadId ?: throw BadRequestException("파일 아이디가 필요합니다.")
            return SubmitActivityReportRequest(isPresentation, fileUploadId)
        }
    }
}
