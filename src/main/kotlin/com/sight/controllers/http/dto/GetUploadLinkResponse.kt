package com.sight.controllers.http.dto

data class GetUploadLinkResponse(
    val url: String,
    val fileKey: String,
    val fileUploadId: String,
)
