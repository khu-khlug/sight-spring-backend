package com.sight.controllers.http.dto

import java.time.LocalDateTime

data class CreateGroupMatchingAnswerResponse(
    val id: String,
    val groupType: String,
    val isPreferOnline: Boolean,
    val groupMatchingFieldIds: List<String>,
    val createdAt: LocalDateTime,
    val groupMatchingSubjectIds: List<String>,
)
