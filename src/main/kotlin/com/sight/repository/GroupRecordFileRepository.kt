package com.sight.repository

import com.sight.domain.group.GroupRecordFile
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRecordFileRepository : JpaRepository<GroupRecordFile, Long> {
    fun findByRecordIdOrderByIdAsc(recordId: Long): List<GroupRecordFile>

    fun findByRecordIdInOrderByIdAsc(recordIds: Collection<Long>): List<GroupRecordFile>

    fun findByDraftIdOrderByIdAsc(draftId: Long): List<GroupRecordFile>

    fun existsByFileUploadId(fileUploadId: String): Boolean

    fun deleteByFileUploadId(fileUploadId: String)
}
