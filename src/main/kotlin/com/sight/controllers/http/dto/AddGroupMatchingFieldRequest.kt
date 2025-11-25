package com.sight.controllers.http.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class AddGroupMatchingFieldRequest(
    @field:NotBlank(message = "관심분야 이름은 필수입니다")
    @field:JsonProperty("fieldName")
    val fieldName: String,
)
