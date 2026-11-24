package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException
import com.sight.core.id.IntegerId

data class UpdateGroupRecordPositionRequest(
    @field:IntegerId
    val targetCardId: String,
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(node: JsonNode): UpdateGroupRecordPositionRequest {
            val value =
                node["targetCardId"]?.takeIf { it.isTextual }?.asText()
                    ?: throw BadRequestException("targetCardId 문자열이 필요합니다.")
            return UpdateGroupRecordPositionRequest(value)
        }
    }
}
