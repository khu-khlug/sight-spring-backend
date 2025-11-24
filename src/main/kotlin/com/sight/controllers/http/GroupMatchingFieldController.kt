package com.sight.controllers.http

import com.sight.controllers.http.dto.AddGroupMatchingFieldRequest
import com.sight.controllers.http.dto.AddGroupMatchingFieldResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.GroupMatchingFieldService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
}
