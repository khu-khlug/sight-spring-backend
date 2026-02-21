package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class ListIdeaCloudResponse(
    val ideaClouds: List<IdeaCloudItemResponse>,
    val count: Int,
)

data class IdeaCloudItemResponse(
    val id: Long,
    val content: String,
    val author: IdeaCloudAuthorResponse,
    val createdAt: LocalDateTime?,
)

data class IdeaCloudAuthorResponse(
    val id: Long,
    val realname: String,
)

data class CreateIdeaCloudRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val content: String,
)

data class CreateIdeaCloudResponse(
    val id: Long,
)
