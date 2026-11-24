package com.sight.repository.dto

import com.sight.domain.file.FileReference
import com.sight.domain.file.FileUpload
import com.sight.domain.group.GroupRecordFile

data class GroupRecordFileView(
    val recordFile: GroupRecordFile,
    val fileReference: FileReference,
    val fileUpload: FileUpload,
)
