package com.sight.repository

import com.sight.domain.group.GroupActivityReport
import org.springframework.data.jpa.repository.JpaRepository

interface GroupActivityReportRepository : JpaRepository<GroupActivityReport, String> {
    fun findByGroupId(groupId: Long): List<GroupActivityReport>

    fun existsByGroupIdAndSeminarId(
        groupId: Long,
        seminarId: String,
    ): Boolean
}
