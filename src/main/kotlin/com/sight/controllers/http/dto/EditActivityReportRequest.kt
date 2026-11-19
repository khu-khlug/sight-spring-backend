package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

data class EditActivityReportRequest(
    val isPresentation: Boolean?,
    val fileUploadId: String?,
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(body: JsonNode): EditActivityReportRequest {
            val fields = ActivityReportRequestFields.parse(body)
            if (fields.isPresentation == null && fields.fileUploadId == null) {
                throw BadRequestException("수정하는 내용이 없습니다.")
            }
            return EditActivityReportRequest(fields.isPresentation, fields.fileUploadId)
        }
    }
}
