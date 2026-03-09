package com.sight.controllers.http.dto

import com.sight.domain.groupmatching.ActivityFrequency
import com.sight.domain.groupmatching.GroupMatchingType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UpdateGroupMatchingAnswerRequest(
    @field:NotNull(message = "그룹 타입은 필수입니다")
    val groupType: GroupMatchingType,

    @field:NotNull(message = "온라인 선호 여부는 필수입니다")
    val isPreferOnline: Boolean,

    @field:NotNull(message = "활동 빈도는 필수입니다")
    val activityFrequency: ActivityFrequency,

    @field:NotBlank(message = "활동 형태는 필수입니다")
    @field:Size(max = 500, message = "활동 형태는 500자 이내여야 합니다")
    val activityFormat: String,

    @field:Size(max = 1000, message = "기타 제안은 1000자 이내여야 합니다")
    val otherSuggestions: String? = null,

    val selectedOptionIds: List<String> = emptyList(),

    @field:Size(max = 255, message = "기타 옵션은 255자 이내여야 합니다")
    val customOption: String? = null,

    @field:Size(max = 100, message = "역할은 100자 이내여야 합니다")
    val role: String? = null,

    val hasIdea: Boolean? = null,

    @field:Size(max = 1000, message = "아이디어는 1000자 이내여야 합니다")
    val idea: String? = null,
)
