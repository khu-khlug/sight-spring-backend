package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

data class UpdateGroupRecordPortfolioRequest(val isInPortfolio: Boolean) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun from(node: JsonNode): UpdateGroupRecordPortfolioRequest {
            val value =
                node["isInPortfolio"]?.takeIf { it.isBoolean }
                    ?: throw BadRequestException("isInPortfolio는 필수 불리언입니다.")
            return UpdateGroupRecordPortfolioRequest(value.booleanValue())
        }
    }
}
