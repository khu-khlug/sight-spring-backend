package com.sight.service

data class RecordFileInput(
    val id: String,
    val fileId: String?,
    val name: String?,
    val size: Long?,
    val contentType: String?,
)

data class RecordFileUploadResult(
    val id: String,
    val fileId: String,
    val url: String,
    val uploadUrl: String,
)

data class RecordFileResult(
    val id: String,
    val fileId: String,
    val url: String?,
    val name: String,
    val size: Long,
    val contentType: String,
)

data class RecordFilesChangeResult(
    val uploads: List<RecordFileUploadResult>,
    val isChanged: Boolean,
)
