package com.sight.repository

import com.sight.domain.group.GroupActivityReport
import com.sight.repository.dto.GroupActivityReportView

interface GroupActivityReportRepositoryCustom {
    fun findLockedByGroupIdAndId(
        groupId: Long,
        id: String,
    ): GroupActivityReport?

    fun findWithFileAndSeminarByGroupId(groupId: Long): List<GroupActivityReportView>
}
