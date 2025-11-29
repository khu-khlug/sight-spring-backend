package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateGroupMatchingAnswerRequest
import com.sight.controllers.http.dto.CreateGroupMatchingAnswerResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.domain.group.GroupCategory
import com.sight.service.GroupMatchingAnswerService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupMatchingAnswerController(
    private val groupMatchingAnswerService: GroupMatchingAnswerService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/group-matchings/{groupMatchingId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroupMatchingAnswer(
        @Valid @RequestBody request: CreateGroupMatchingAnswerRequest,
        @PathVariable groupMatchingId: String,
        userId: Long,
    ): CreateGroupMatchingAnswerResponse {
        val result =
            groupMatchingAnswerService.createGroupMatchingAnswer(
                groupType = GroupCategory.valueOf(request.groupType),
                isPreferOnline = request.isPreferOnline,
                userId = userId,
                groupMatchingFieldIds = request.groupMatchingFieldIds,
                groupMatchingSubjects = request.groupMatchingSubjects,
                groupMatchingId = groupMatchingId,
            )

        return CreateGroupMatchingAnswerResponse(
            id = result.answer.id,
            groupType = result.answer.groupType.toString(),
            isPreferOnline = result.answer.isPreferOnline,
            createdAt = result.answer.createdAt,
            groupMatchingFieldIds = result.fieldIds,
            groupMatchingSubjectIds = result.subjectIds,
        )
    }
}
