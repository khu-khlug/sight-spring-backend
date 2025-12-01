package com.sight.service.dto

import com.sight.domain.group.GroupCategory

data class UpdateGroupMatchingAnswerDto(
    val groupType: GroupCategory,
    val isPreferOnline: Boolean,
    val fieldIds: List<String>,
    val subjects: List<String>,
)
