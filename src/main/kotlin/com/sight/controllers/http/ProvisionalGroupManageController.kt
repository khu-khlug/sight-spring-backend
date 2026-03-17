package com.sight.controllers.http

import com.sight.controllers.http.dto.AssignProvisionalGroupRequest
import com.sight.controllers.http.dto.CreateProvisionalGroupRequest
import com.sight.controllers.http.dto.CreateProvisionalGroupResponse
import com.sight.controllers.http.dto.ListProvisionalGroupsResponse
import com.sight.controllers.http.dto.ProvisionalGroupDto
import com.sight.controllers.http.dto.UpdateProvisionalGroupRequest
import com.sight.controllers.http.dto.UpdateProvisionalGroupResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.ProvisionalGroupService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ProvisionalGroupManageController(
    private val provisionalGroupService: ProvisionalGroupService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/provisional-groups")
    fun listProvisionalGroups(
        @PathVariable groupMatchingId: String,
    ): ListProvisionalGroupsResponse {
        val result = provisionalGroupService.listProvisionalGroups(groupMatchingId)

        return ListProvisionalGroupsResponse(
            provisionalGroups =
                result.provisionalGroups.map { detail ->
                    ProvisionalGroupDto(
                        id = detail.id,
                        name = detail.name,
                        groupMatchingId = detail.groupMatchingId,
                        answerCount = detail.answerCount,
                        createdAt = detail.createdAt,
                    )
                },
        )
    }

    @Auth([UserRole.MANAGER])
    @PostMapping("/group-matchings/{groupMatchingId}/provisional-groups")
    @ResponseStatus(HttpStatus.CREATED)
    fun createProvisionalGroup(
        @PathVariable groupMatchingId: String,
        @Valid @RequestBody request: CreateProvisionalGroupRequest,
    ): CreateProvisionalGroupResponse {
        val detail = provisionalGroupService.createProvisionalGroup(groupMatchingId, request.name)

        return CreateProvisionalGroupResponse(
            id = detail.id,
            name = detail.name,
            groupMatchingId = detail.groupMatchingId,
            answerCount = detail.answerCount,
            createdAt = detail.createdAt,
        )
    }

    @Auth([UserRole.MANAGER])
    @PatchMapping("/group-matchings/{groupMatchingId}/provisional-groups/{provisionalGroupId}")
    fun updateProvisionalGroup(
        @PathVariable groupMatchingId: String,
        @PathVariable provisionalGroupId: String,
        @Valid @RequestBody request: UpdateProvisionalGroupRequest,
    ): UpdateProvisionalGroupResponse {
        val detail = provisionalGroupService.updateProvisionalGroup(groupMatchingId, provisionalGroupId, request.name)

        return UpdateProvisionalGroupResponse(
            id = detail.id,
            name = detail.name,
            groupMatchingId = detail.groupMatchingId,
            answerCount = detail.answerCount,
            createdAt = detail.createdAt,
        )
    }

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/group-matchings/{groupMatchingId}/provisional-groups/{provisionalGroupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProvisionalGroup(
        @PathVariable groupMatchingId: String,
        @PathVariable provisionalGroupId: String,
    ) {
        provisionalGroupService.deleteProvisionalGroup(groupMatchingId, provisionalGroupId)
    }

    @Auth([UserRole.MANAGER])
    @PatchMapping("/group-matchings/{groupMatchingId}/answers/{answerId}/provisional-group")
    fun assignProvisionalGroup(
        @PathVariable groupMatchingId: String,
        @PathVariable answerId: String,
        @RequestBody request: AssignProvisionalGroupRequest,
    ) {
        provisionalGroupService.assignProvisionalGroup(groupMatchingId, answerId, request.provisionalGroupId)
    }
}
