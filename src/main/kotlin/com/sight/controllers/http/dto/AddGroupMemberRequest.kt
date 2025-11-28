package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class AddGroupMemberRequest(
    @field:NotBlank
    val method: String,

    @JsonProperty("groupMatchingParams")
    val groupMatchingParams: GroupMatchingParams?,
)

data class GroupMatchingParams(
    @field:NotBlank
    val answerId: String,
)
