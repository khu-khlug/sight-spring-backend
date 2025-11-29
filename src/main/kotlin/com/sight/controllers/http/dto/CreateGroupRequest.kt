package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateGroupRequest(
    @field:NotBlank
    val method: String,

    @field:NotBlank
    val title: String,

    val groupMatchingParams: CreateGroupMatchingParams?,
)

data class CreateGroupMatchingParams(
    @field:NotNull
    @field:Size(min = 1)
    val answerIds: List<String>,

    @field:NotNull
    val leaderUserId: Long,
)
