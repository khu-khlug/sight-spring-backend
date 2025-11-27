package com.sight.repository

import com.sight.domain.groupmatching.MatchedGroup
import org.springframework.data.jpa.repository.JpaRepository

interface MatchedGroupRepository : JpaRepository<MatchedGroup, String> {
    fun findAllByAnswerIdIn(answerIds: List<String>): List<MatchedGroup>

    fun existsByGroupIdAndAnswerId(
        groupId: Long,
        answerId: String,
    ): Boolean
}
