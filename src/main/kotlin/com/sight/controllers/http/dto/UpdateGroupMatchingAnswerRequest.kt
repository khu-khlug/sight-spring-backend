package com.sight.controllers.http.dto

import com.sight.domain.group.GroupCategory

data class UpdateGroupMatchingAnswerRequest(
    val groupType: GroupCategory,
    val isPreferOnline: Boolean,
    val fieldIds: List<String>,
    val subjects: List<String>,
)
