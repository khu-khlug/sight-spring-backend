package com.sight.repository

import com.sight.domain.groupmatching.GroupMatchingAnswerOption
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMatchingAnswerOptionRepository : JpaRepository<GroupMatchingAnswerOption, String> {
    fun findAllByAnswerId(answerId: String): List<GroupMatchingAnswerOption>

    fun deleteAllByAnswerId(answerId: String)
}
