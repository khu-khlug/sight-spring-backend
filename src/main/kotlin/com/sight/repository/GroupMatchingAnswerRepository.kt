package com.sight.repository

import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupMatchingAnswerRepository : JpaRepository<GroupMatchingAnswer, String> {
    fun findAllByGroupMatchingId(groupMatchingId: String): List<GroupMatchingAnswer>

    fun existsByUserIdAndGroupMatchingId(
        userId: Long,
        groupMatchingId: String,
    ): Boolean

    fun findAllByGroupMatchingIdAndGroupType(
        groupMatchingId: String,
        groupType: GroupMatchingType,
    ): List<GroupMatchingAnswer>

    @Query(
        """
        SELECT DISTINCT a FROM GroupMatchingAnswer a
        LEFT JOIN GroupMatchingAnswerOption ao ON a.id = ao.answerId
        WHERE a.groupMatchingId = :groupMatchingId
        AND (:groupType IS NULL OR a.groupType = :groupType)
        AND (:optionId IS NULL OR ao.optionId = :optionId)
        ORDER BY a.createdAt DESC
        """,
    )
    fun findAnswersWithFilters(
        @Param("groupMatchingId") groupMatchingId: String,
        @Param("groupType") groupType: GroupMatchingType?,
        @Param("optionId") optionId: String?,
        pageable: Pageable,
    ): Page<GroupMatchingAnswer>

    fun findByGroupMatchingIdAndUserId(
        groupMatchingId: String,
        userId: Long,
    ): GroupMatchingAnswer?
}
