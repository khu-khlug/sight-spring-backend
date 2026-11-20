package com.sight.repository

import com.sight.repository.dto.GroupActivityReportFileView

interface GroupActivityReportRepositoryCustom {
    fun findWithFilesByGroupId(groupId: Long): List<GroupActivityReportFileView>
}
