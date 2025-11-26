package com.sight.repository

import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatchingAnswer
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMatchingAnswerRepository : JpaRepository<GroupMatchingAnswer, String> {
    fun findAllByGroupMatchingId(groupMatchingId: String): List<GroupMatchingAnswer>

    fun findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId: String): List<GroupMatchingAnswer>

    fun findAllByGroupMatchingIdAndGroupType(
        groupMatchingId: String,
        groupType: GroupCategory,
    ): List<GroupMatchingAnswer>
}
