package com.sight.repository

import com.sight.domain.groupmatching.MatchedGroup
import org.springframework.data.jpa.repository.JpaRepository

<<<<<<< Updated upstream
interface MatchedGroupRepository : JpaRepository<MatchedGroup, String>
=======
interface MatchedGroupRepository : JpaRepository<MatchedGroup, String> {
    fun findAllByAnswerIdIn(answerIds: List<String>): List<MatchedGroup>

    fun findAllByAnswerId(answerId: String): List<MatchedGroup>
}
>>>>>>> Stashed changes
