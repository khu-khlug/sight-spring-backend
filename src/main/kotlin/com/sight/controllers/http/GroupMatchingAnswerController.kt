package com.sight.controllers.http

import com.sight.controllers.http.dto.AnswerDto
import com.sight.controllers.http.dto.GetAnswersResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupCategory
import com.sight.service.GroupMatchingAnswerService
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class GroupMatchingAnswerController(
    private val answerService: GroupMatchingAnswerService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/answers")
    fun getAnswers(
        @PathVariable groupMatchingId: String,
        @RequestParam(required = false) groupType: String?,
        @RequestParam(required = false) fieldId: String?,
        @RequestParam(required = false, defaultValue = "0")
        @Min(0, message = "offset은 0 이상이어야 합니다")
        offset: Int,
        @RequestParam(required = false, defaultValue = "20")
        @Min(1, message = "limit은 양의 정수여야 합니다")
        limit: Int,
    ): GetAnswersResponse {
        // Controller에서 groupType 검증 및 변환
        val groupCategory: GroupCategory? =
            groupType?.let {
                when (it.uppercase()) {
                    "STUDY" -> GroupCategory.STUDY
                    "PROJECT" -> GroupCategory.PROJECT
                    else -> throw BadRequestException("유효하지 않은 그룹 타입입니다")
                }
            }

        val result = answerService.getAllAnswers(groupMatchingId, groupCategory, fieldId, offset, limit)

        // service.dto를 controllers.http.dto로 변환
        return GetAnswersResponse(
            answers =
                result.answers.map { summary ->
                    AnswerDto(
                        answerId = summary.answerId,
                        answerUserId = summary.answerUserId,
                        createdAt = summary.createdAt,
                        updatedAt = summary.updatedAt,
                        groupType = summary.groupType,
                        isPreferOnline = summary.isPreferOnline,
                        selectedFields = summary.selectedFields,
                        subjectIdeas = summary.subjectIdeas,
                        matchedGroupIds = summary.matchedGroupIds,
                    )
                },
            total = result.total,
        )
    }
}
