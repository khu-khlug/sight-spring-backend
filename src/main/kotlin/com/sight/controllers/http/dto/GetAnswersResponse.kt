package com.sight.controllers.http.dto

import com.sight.domain.group.GroupCategory
import java.time.LocalDateTime

data class GetAnswersResponse(
    val answers: List<AnswerDto>,
    val total: Int,
)

data class AnswerDto(
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
