package com.sight.service.dto

import com.sight.domain.groupmatching.ActivityFrequency
import com.sight.domain.groupmatching.GroupMatchingType

data class UpdateGroupMatchingAnswerDto(
    val groupType: GroupMatchingType,
    val isPreferOnline: Boolean,
    val activityFrequency: ActivityFrequency,
    val activityFormat: String,
    val otherSuggestions: String?,
    val selectedOptionIds: List<String>,
    val customOption: String?,
    val role: String?,
    val hasIdea: Boolean?,
    val idea: String?,
)
