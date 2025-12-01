package com.sight.service.dto

import com.sight.domain.group.GroupCategory
import java.time.LocalDateTime

data class ListAnswersResult(
    val answers: List<AnswerSummary>,
    val total: Int,
)

data class AnswerSummary(
    val answerId: String,
    val answerUserId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val groupType: GroupCategory,
    val isPreferOnline: Boolean,
    val selectedFields: List<String>,
    val subjectIdeas: List<String>,
    val matchedGroupIds: List<Long>,
)
