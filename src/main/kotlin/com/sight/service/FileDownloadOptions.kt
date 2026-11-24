package com.sight.service

import java.time.Instant

data class FileDownloadOptions(
    val fileName: String,
    val contentType: String,
    val inline: Boolean,
)

data class SignedFileUrl(val url: String, val expiresAt: Instant)
