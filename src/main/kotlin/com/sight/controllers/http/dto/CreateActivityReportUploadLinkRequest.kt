package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

data class CreateActivityReportUploadLinkRequest(val size: Long) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(body: JsonNode): CreateActivityReportUploadLinkRequest {
            val size =
                body["size"]?.takeIf { it.isIntegralNumber && it.canConvertToLong() }?.longValue()
                    ?: throw BadRequestException("파일 크기는 필수 정수입니다.")
            return CreateActivityReportUploadLinkRequest(size)
        }
    }
}
