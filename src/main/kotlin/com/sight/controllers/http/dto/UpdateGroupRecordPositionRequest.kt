package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

data class UpdateGroupRecordPositionRequest(val targetCardId: String) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(node: JsonNode): UpdateGroupRecordPositionRequest {
            val value =
                node["targetCardId"]?.takeIf { it.isTextual }?.asText()
                    ?: throw BadRequestException("targetCardId 문자열이 필요합니다.")
            if (!Regex("[1-9][0-9]{0,18}").matches(value) || value.toLongOrNull() == null) {
                throw BadRequestException("targetCardId는 양의 Long 10진 문자열이어야 합니다.")
            }
            return UpdateGroupRecordPositionRequest(value)
        }
    }
}
