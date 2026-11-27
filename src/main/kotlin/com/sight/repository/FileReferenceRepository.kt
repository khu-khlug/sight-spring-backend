package com.sight.repository

import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType

interface FileReferenceRepository {
    fun findById(id: String): FileReference?

    fun findByRefTypeAndReferrerOrderById(
        referenceType: FileReferenceType,
        referrerId: String,
        cursor: String,
        limit: Int,
    ): List<FileReference>

    fun existsByFileUploadId(fileUploadId: String): Boolean

    fun findDistinctTypesByFileUploadId(fileUploadId: String): Set<FileReferenceType>

    fun findByFileUploadIdOrderById(
        fileUploadId: String,
        cursor: String,
        limit: Int,
    ): List<FileReference>

    fun save(reference: FileReference): FileReference

    fun deleteByIdAndRefTypeAndReferrer(
        id: String,
        referenceType: FileReferenceType,
        referrerId: String,
    ): Boolean
}
