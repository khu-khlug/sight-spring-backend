package com.sight.controllers.http

import com.sight.controllers.http.dto.AddGroupMatchingFieldRequest
import com.sight.controllers.http.dto.AddGroupMatchingFieldResponse
import com.sight.controllers.http.dto.GetGroupMatchingFieldResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupMatchingFieldService
import com.sight.service.dto.GroupMatchingFieldAnswer
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupMatchingFieldController(
    private val groupMatchingFieldService: GroupMatchingFieldService,
) {
    @Auth([UserRole.MANAGER])
    @PostMapping("/fields")
    @ResponseStatus(HttpStatus.CREATED)
    fun addGroupMatchingField(
        @Valid @RequestBody request: AddGroupMatchingFieldRequest,
    ): AddGroupMatchingFieldResponse {
        val field = groupMatchingFieldService.addGroupMatchingField(request)

        return AddGroupMatchingFieldResponse(
            fieldId = field.id,
            fieldName = field.name,
            createdAt = field.createdAt,
        )
    }

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/fields/{fieldId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGroupMatchingField(
        @PathVariable fieldId: String,
    ) {
        groupMatchingFieldService.deleteGroupMatchingField(fieldId)
    }

    @Auth(roles = [UserRole.MANAGER, UserRole.USER])
    @GetMapping("/fields")
    fun getGroupMatchingFields(requester: Requester): List<GetGroupMatchingFieldResponse> {
        val fields: List<GroupMatchingFieldAnswer> = groupMatchingFieldService.getGroupMatchingFields(requester.role)

        return fields.map { field ->
            GetGroupMatchingFieldResponse(
                id = field.id,
                name = field.name,
                createdAt = field.createdAt,
                obsoletedAt = field.obsoletedAt,
            )
        }
    }
}
