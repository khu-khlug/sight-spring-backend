package com.sight.repository

import com.sight.domain.file.FileReferenceType
import com.sight.domain.group.GroupRecordFile
import com.sight.repository.dto.GroupRecordFileView
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRecordFileRepository : JpaRepository<GroupRecordFile, Long>, GroupRecordFileRepositoryCustom

interface GroupRecordFileRepositoryCustom {
    fun findByFileUploadIdOrderById(fileUploadId: String): List<GroupRecordFileView>

    fun findByTypeAndOwnerOrderById(
        type: FileReferenceType,
        ownerId: String,
    ): List<GroupRecordFileView>

    fun findByTypeAndOwnerInOrderById(
        type: FileReferenceType,
        ownerIds: Collection<String>,
    ): List<GroupRecordFileView>
}
