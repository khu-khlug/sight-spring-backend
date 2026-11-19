package com.sight.repository

import com.sight.domain.group.GroupFile
import org.springframework.data.jpa.repository.JpaRepository

interface GroupFileRepository : JpaRepository<GroupFile, Long> {
    fun existsByGroupIdAndFileUploadId(
        groupId: Long,
        fileUploadId: String,
    ): Boolean

    fun deleteByFileUploadId(fileUploadId: String)
}
