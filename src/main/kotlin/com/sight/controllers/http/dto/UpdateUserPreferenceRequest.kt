package com.sight.controllers.http.dto

import com.fasterxml.jackson.databind.JsonNode
import com.sight.core.exception.BadRequestException

data class UpdateUserPreferenceRequest(
    val usePersonalGithubUsage: JsonNode? = null,
) {
    fun personalGithubUsageValue(): Boolean =
        usePersonalGithubUsage
            ?.takeIf { it.isBoolean }
            ?.booleanValue()
            ?: throw BadRequestException("개인 GitHub API 사용 여부는 불리언으로 입력해야 합니다.")
}
