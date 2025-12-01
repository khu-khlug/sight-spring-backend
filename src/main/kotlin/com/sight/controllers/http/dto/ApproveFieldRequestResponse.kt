package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class ApproveFieldRequestResponse(
    val field: FieldInfo,
    val approvedAt: LocalDateTime,
)

data class FieldInfo(
    val id: String,
    val name: String,
    val createdAt: LocalDateTime,
)
