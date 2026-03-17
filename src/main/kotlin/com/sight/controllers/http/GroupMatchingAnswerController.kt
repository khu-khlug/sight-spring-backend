package com.sight.controllers.http

import com.sight.controllers.http.dto.AnswerDto
import com.sight.controllers.http.dto.AnswerOptionDto
import com.sight.controllers.http.dto.CreateGroupMatchingAnswerRequest
import com.sight.controllers.http.dto.CreateGroupMatchingAnswerResponse
import com.sight.controllers.http.dto.GetAnswersResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.groupmatching.GroupMatchingType
import com.sight.service.GroupMatchingAnswerService
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class GroupMatchingAnswerController(
    private val groupMatchingAnswerService: GroupMatchingAnswerService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/group-matchings/{groupMatchingId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroupMatchingAnswer(
        @Valid @RequestBody request: CreateGroupMatchingAnswerRequest,
        @PathVariable groupMatchingId: String,
        requester: Requester,
    ): CreateGroupMatchingAnswerResponse {
        val result =
            groupMatchingAnswerService.createGroupMatchingAnswer(
                groupType = request.groupType,
                isPreferOnline = request.isPreferOnline,
                activityFrequency = request.activityFrequency,
                activityFormat = request.activityFormat,
                otherSuggestions = request.otherSuggestions,
                userId = requester.userId,
                groupMatchingId = groupMatchingId,
                selectedOptionIds = request.selectedOptionIds,
                customOption = request.customOption,
                role = request.role,
                hasIdea = request.hasIdea,
                idea = request.idea,
            )

        return CreateGroupMatchingAnswerResponse(
            id = result.answer.id,
            userId = result.answer.userId,
            groupMatchingId = result.answer.groupMatchingId,
            groupType = result.answer.groupType,
            isPreferOnline = result.answer.isPreferOnline,
            activityFrequency = result.answer.activityFrequency,
            activityFormat = result.answer.activityFormat,
            otherSuggestions = result.answer.otherSuggestions,
            selectedOptions = result.options.map { CreateGroupMatchingAnswerResponse.OptionResponse(id = it.id, name = it.name) },
            customOption = result.answer.customOption,
            role = result.answer.role,
            hasIdea = result.answer.hasIdea,
            idea = result.answer.idea,
            matchedGroups = emptyList(),
            createdAt = result.answer.createdAt,
            updatedAt = result.answer.updatedAt,
        )
    }

    @Auth([UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/answers")
    fun getAnswers(
        @PathVariable groupMatchingId: String,
        @RequestParam(required = false) groupType: GroupMatchingType?,
        @RequestParam(required = false) optionId: String?,
        @RequestParam(required = false, defaultValue = "0")
        @Min(0, message = "offset은 0 이상이어야 합니다")
        offset: Int,
        @RequestParam(required = false, defaultValue = "20")
        @Min(1, message = "limit은 양의 정수여야 합니다")
        limit: Int,
        @RequestParam(required = false) provisionalGroupId: String?,
    ): GetAnswersResponse {
        val result = groupMatchingAnswerService.listAnswers(groupMatchingId, groupType, optionId, provisionalGroupId, offset, limit)

        return GetAnswersResponse(
            answers =
                result.answers.map { summary ->
                    AnswerDto(
                        answerId = summary.answerId,
                        answerUserId = summary.answerUserId,
                        answerUserName = summary.answerUserName,
                        answerUserNumber = summary.answerUserNumber,
                        createdAt = summary.createdAt,
                        updatedAt = summary.updatedAt,
                        groupType = summary.groupType,
                        isPreferOnline = summary.isPreferOnline,
                        activityFrequency = summary.activityFrequency,
                        activityFormat = summary.activityFormat,
                        otherSuggestions = summary.otherSuggestions,
                        selectedOptions =
                            summary.selectedOptions.map { opt ->
                                AnswerOptionDto(id = opt.id, name = opt.name)
                            },
                        customOption = summary.customOption,
                        role = summary.role,
                        hasIdea = summary.hasIdea,
                        idea = summary.idea,
                        matchedGroupIds = summary.matchedGroupIds,
                        provisionalGroupId = summary.provisionalGroupId,
                        provisionalGroupName = summary.provisionalGroupName,
                    )
                },
            count = result.count,
        )
    }
}
