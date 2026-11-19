package com.sight.repository

import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType

interface FileReferenceRepository {
    fun findById(id: String): FileReference?

    fun findByTypeAndOwnerOrderById(
        referenceType: FileReferenceType,
        ownerId: String,
        cursor: String,
        limit: Int,
    ): List<FileReference>

    fun existsByFileUploadId(fileUploadId: String): Boolean

    fun findDistinctTypesByFileUploadId(fileUploadId: String): Set<FileReferenceType>

    fun save(reference: FileReference): FileReference

    fun deleteByIdAndTypeAndOwner(
        id: String,
        referenceType: FileReferenceType,
        ownerId: String,
    ): Boolean
}
