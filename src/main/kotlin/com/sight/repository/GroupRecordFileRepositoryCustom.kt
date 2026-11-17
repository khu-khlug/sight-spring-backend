package com.sight.repository

import com.sight.domain.file.FileReferenceType
import com.sight.repository.dto.GroupRecordFileView

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
