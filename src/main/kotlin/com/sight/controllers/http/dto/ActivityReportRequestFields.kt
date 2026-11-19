package com.sight.controllers.http.dto

import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

internal data class ActivityReportRequestFields(
    val isPresentation: Boolean?,
    val fileUploadId: String?,
) {
    companion object {
        fun parse(body: JsonNode): ActivityReportRequestFields {
            if (!body.isObject) throw BadRequestException("요청 본문은 객체여야 합니다.")
            val presentation = body.get("isPresentation")?.takeUnless { it.isNull }
            if (presentation != null && !presentation.isBoolean) {
                throw BadRequestException("발표 여부는 불리언이어야 합니다.")
            }
            val file = body.get("fileUploadId")?.takeUnless { it.isNull }
            if (file != null && !file.isTextual) throw BadRequestException("파일 아이디는 문자열이어야 합니다.")
            return ActivityReportRequestFields(presentation?.booleanValue(), file?.textValue())
        }
    }
}
