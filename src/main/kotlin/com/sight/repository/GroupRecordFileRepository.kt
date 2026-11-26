package com.sight.repository

import com.sight.domain.file.FileReferenceType
import com.sight.domain.group.GroupRecordFile
import com.sight.repository.dto.GroupRecordFileView
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRecordFileRepository : JpaRepository<GroupRecordFile, Long>, GroupRecordFileRepositoryCustom {
    fun findByFileReferenceId(fileReferenceId: String): GroupRecordFile?
}

interface GroupRecordFileRepositoryCustom {
    fun findByFileUploadIdOrderById(fileUploadId: String): List<GroupRecordFileView>

    fun findByRefTypeAndReferrerOrderById(
        refType: FileReferenceType,
        referrerId: String,
    ): List<GroupRecordFileView>

    fun findByRefTypeAndReferrerInOrderById(
        refType: FileReferenceType,
        referrerIds: Collection<String>,
    ): List<GroupRecordFileView>
}
