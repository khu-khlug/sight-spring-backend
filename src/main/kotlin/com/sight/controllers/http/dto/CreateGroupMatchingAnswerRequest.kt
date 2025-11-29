package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class CreateGroupMatchingAnswerRequest(
    @field:NotBlank
    val groupType: String,
    @field:NotNull
    val isPreferOnline: Boolean,
    @field:NotEmpty
    val groupMatchingFieldIds: List<String>,
    val groupMatchingSubjects: List<String> = emptyList(),
)
