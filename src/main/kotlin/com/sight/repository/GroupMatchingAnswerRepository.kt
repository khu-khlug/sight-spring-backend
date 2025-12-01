package com.sight.repository

import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatchingAnswer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupMatchingAnswerRepository : JpaRepository<GroupMatchingAnswer, String> {
    fun findAllByGroupMatchingId(groupMatchingId: String): List<GroupMatchingAnswer>

    fun findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId: String): List<GroupMatchingAnswer>

    fun findAllByGroupMatchingIdAndGroupType(
        groupMatchingId: String,
        groupType: GroupCategory,
    ): List<GroupMatchingAnswer>

    @Query(
        """
        SELECT DISTINCT a FROM GroupMatchingAnswer a
        LEFT JOIN GroupMatchingAnswerField af ON a.id = af.answerId
        WHERE a.groupMatchingId = :groupMatchingId
        AND (:groupType IS NULL OR a.groupType = :groupType)
        AND (:fieldId IS NULL OR af.fieldId = :fieldId)
        ORDER BY a.createdAt DESC
        """,
    )
    fun findAnswersWithFilters(
        @Param("groupMatchingId") groupMatchingId: String,
        @Param("groupType") groupType: GroupCategory?,
        @Param("fieldId") fieldId: String?,
        pageable: Pageable,
    ): Page<GroupMatchingAnswer>

    fun findByGroupMatchingIdAndUserId(
        groupMatchingId: String,
        userId: Long,
    ): GroupMatchingAnswer?
}
