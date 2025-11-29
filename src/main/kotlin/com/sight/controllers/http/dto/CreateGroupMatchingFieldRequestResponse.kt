package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class CreateGroupMatchingFieldRequestResponse(
    val id: String,
    val fieldName: String,
    val requestReason: String? = null,
    val createdAt: LocalDateTime,
)
