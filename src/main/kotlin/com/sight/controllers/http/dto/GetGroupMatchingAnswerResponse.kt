package com.sight.controllers.http.dto

import com.sight.domain.groupmatching.ActivityFrequency
import com.sight.domain.groupmatching.GroupMatchingType
import java.time.LocalDateTime

data class GetGroupMatchingAnswerResponse(
    val id: String,
    val userId: Long,
    val groupMatchingId: String,
    val groupType: GroupMatchingType,
    val isPreferOnline: Boolean,
    val activityFrequency: ActivityFrequency,
    val activityFormat: String,
    val otherSuggestions: String?,
    val selectedOptions: List<OptionResponse>,
    val customOption: String?,
    val role: String?,
    val hasIdea: Boolean?,
    val idea: String?,
    val matchedGroups: List<MatchedGroupResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    data class OptionResponse(
        val id: String,
        val name: String,
    )

    data class MatchedGroupResponse(
        val id: String,
        val groupId: Long,
        val createdAt: LocalDateTime,
    )
}
