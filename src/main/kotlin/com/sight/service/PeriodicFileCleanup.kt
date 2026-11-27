package com.sight.service

import com.sight.domain.file.FileReference
import com.sight.domain.file.FileReferenceType
import com.sight.domain.file.FileUpload

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class PeriodicFileCleanup(vararg val referenceTypes: FileReferenceType)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class BeforePeriodicFileCleanup(vararg val referenceTypes: FileReferenceType)

data class FileCleanupContext(
    val upload: FileUpload,
    val reference: FileReference,
    val objectExists: Boolean?,
)

data class FileCleanupBinding(
    val referenceType: FileReferenceType,
    val isCleanupTarget: (FileCleanupContext) -> Boolean,
    val releaseDetails: ((FileReference) -> Unit)? = null,
)

interface FileCleanupRegistry {
    val bindings: List<FileCleanupBinding>
}
