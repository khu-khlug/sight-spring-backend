package com.sight.domain.file

enum class FileUploadPurpose {
    RECORD_ATTACHMENT,
    SINGLE_USE,
    ;

    fun isInUse(
        isUploaded: Boolean,
        hasRecordLinks: Boolean,
    ): Boolean = if (this == RECORD_ATTACHMENT) hasRecordLinks else isUploaded

    companion object {
        private val recordPath = Regex("/groups/([1-9][0-9]{0,18})/records/files")

        fun fromApiPath(apiPath: String): FileUploadPurpose {
            val match = recordPath.matchEntire(apiPath)
            return if (match != null && match.groupValues[1].toLongOrNull() != null) RECORD_ATTACHMENT else SINGLE_USE
        }
    }
}
