package com.sight.repository

import com.sight.domain.group.GroupActivityReport
import com.sight.repository.projection.GroupActivityReportCountProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupActivityReportRepository : JpaRepository<GroupActivityReport, String> {
    fun findByGroupId(groupId: Long): List<GroupActivityReport>

    fun existsByGroupIdAndSeminarId(
        groupId: Long,
        seminarId: String,
    ): Boolean

    // 그룹의 활동보고를 발표 보고와 보고서 제출로 나눈 수를 한 번에 센다. 활동보고가 없으면 둘 다 0이다.
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN r.isPresentation = true THEN 1 ELSE 0 END), 0) AS presentationCount,
            COALESCE(SUM(CASE WHEN r.isPresentation = false THEN 1 ELSE 0 END), 0) AS reportCount
        FROM GroupActivityReport r
        WHERE r.groupId = :groupId
        """,
    )
    fun countPresentationsAndReportsByGroupId(
        @Param("groupId") groupId: Long,
    ): GroupActivityReportCountProjection
}
