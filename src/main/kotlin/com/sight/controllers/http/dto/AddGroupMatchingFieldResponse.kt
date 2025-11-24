package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class AddGroupMatchingFieldResponse(
    val fieldId: String,
    val fieldName: String,
    val createdAt: LocalDateTime,
)
