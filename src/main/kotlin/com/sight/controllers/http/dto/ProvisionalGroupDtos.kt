package com.sight.controllers.http.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateProvisionalGroupRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    @field:Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    val name: String,
)

data class UpdateProvisionalGroupRequest(
    @field:NotBlank(message = "이름은 필수입니다.")
    @field:Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    val name: String,
)

data class AssignProvisionalGroupRequest(
    val provisionalGroupId: String?,
)

data class ProvisionalGroupDto(
    val id: String,
    val name: String,
    val groupMatchingId: String,
    val answerCount: Long,
    val createdAt: Instant,
)

data class ListProvisionalGroupsResponse(
    val provisionalGroups: List<ProvisionalGroupDto>,
)

data class CreateProvisionalGroupResponse(
    val id: String,
    val name: String,
    val groupMatchingId: String,
    val answerCount: Long,
    val createdAt: Instant,
)

data class UpdateProvisionalGroupResponse(
    val id: String,
    val name: String,
    val groupMatchingId: String,
    val answerCount: Long,
    val createdAt: Instant,
)
