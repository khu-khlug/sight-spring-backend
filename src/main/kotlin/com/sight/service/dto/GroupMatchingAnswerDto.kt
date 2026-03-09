package com.sight.service.dto

import com.sight.domain.groupmatching.ActivityFrequency
import com.sight.domain.groupmatching.GroupMatchingType
import java.time.LocalDateTime

data class GroupMatchingAnswerDto(
    val id: String,
    val userId: Long,
    val groupType: GroupMatchingType,
    val isPreferOnline: Boolean,
    val activityFrequency: ActivityFrequency,
    val activityFormat: String,
    val otherSuggestions: String?,
    val groupMatchingId: String,
    val selectedOptions: List<OptionResult>,
    val customOption: String?,
    val role: String?,
    val hasIdea: Boolean?,
    val idea: String?,
    val matchedGroups: List<MatchedGroupResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class OptionResult(
    val id: String,
    val name: String,
)

data class MatchedGroupResponse(
    val id: String,
    val groupId: Long,
    val createdAt: LocalDateTime,
)
