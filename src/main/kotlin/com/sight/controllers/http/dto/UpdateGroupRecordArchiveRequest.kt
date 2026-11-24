package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

data class UpdateGroupRecordArchiveRequest(val isInUserArchive: Boolean) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(node: JsonNode): UpdateGroupRecordArchiveRequest {
            val value =
                node["isInUserArchive"]?.takeIf { it.isBoolean }
                    ?: throw BadRequestException("isInUserArchive는 필수 불리언입니다.")
            return UpdateGroupRecordArchiveRequest(value.booleanValue())
        }
    }
}
