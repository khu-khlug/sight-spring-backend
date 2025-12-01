package com.sight.controllers.http.dto

import com.sight.domain.group.GroupCategory
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UpdateGroupMatchingAnswerRequest(
    @field:NotNull(message = "그룹 타입은 필수입니다")
    val groupType: GroupCategory,

    @field:NotNull(message = "온라인 선호 여부는 필수입니다")
    val isPreferOnline: Boolean,

    @field:NotEmpty(message = "관심분야는 최소 1개 이상 선택해야 합니다")
    @field:Size(min = 1, message = "관심분야는 최소 1개 이상 선택해야 합니다")
    val fieldIds: List<String>,

    val subjects: List<String> = emptyList(),
)
