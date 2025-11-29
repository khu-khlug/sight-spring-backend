package com.sight.repository

import com.sight.domain.groupmatching.GroupMatchingAnswerField
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMatchingAnswerFieldRepository : JpaRepository<GroupMatchingAnswerField, String> {
    fun findAllByAnswerId(answerId: String): List<GroupMatchingAnswerField>
}
