package com.sight.service.dto

import com.sight.domain.groupmatching.ActivityFrequency
import com.sight.domain.groupmatching.GroupMatchingType
import java.time.LocalDateTime

data class ListAnswersResult(
    val answers: List<AnswerSummary>,
    val count: Int,
)

data class AnswerSummary(
    val answerId: String,
    val answerUserId: Long,
    val answerUserName: String,
    val answerUserNumber: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val groupType: GroupMatchingType,
    val isPreferOnline: Boolean,
    val activityFrequency: ActivityFrequency,
    val activityFormat: String,
    val otherSuggestions: String?,
    val selectedOptions: List<OptionResult>,
    val customOption: String?,
    val role: String?,
    val hasIdea: Boolean?,
    val idea: String?,
    val matchedGroupIds: List<Long>,
)
