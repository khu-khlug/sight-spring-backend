package com.sight.controllers.http.dto

import com.sight.domain.group.GroupCategory
import java.time.LocalDateTime

data class GetGroupMatchingAnswerResponse(
    val id: String,
    val userId: Long,
    val groupType: GroupCategory,
    val isPreferOnline: Boolean,
    val groupMatchingId: String,
    val fields: List<FieldResponse>,
    val matchedGroups: List<MatchedGroupResponse>,
    val groupMatchingSubjects: List<GroupMatchingSubjectResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    data class FieldResponse(
        val id: String,
        val name: String,
    )

    data class MatchedGroupResponse(
        val id: String,
        val groupId: Long,
        val createdAt: LocalDateTime,
    )

    data class GroupMatchingSubjectResponse(
        val id: String,
        val subject: String,
    )
}
