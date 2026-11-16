package com.sight.service

import java.time.Instant

data class FileUploadCreatedEvent(val fileUploadId: String, val uploadCheckAt: Instant)

data class FileUploadDeletedEvent(val fileUploadId: String)
