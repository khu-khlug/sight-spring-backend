package com.sight.repository

import com.sight.domain.group.GroupFile
import org.springframework.data.jpa.repository.JpaRepository

interface GroupFileRepository : JpaRepository<GroupFile, Long> {
    fun deleteByFileUploadId(fileUploadId: String)
}
