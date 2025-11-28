package com.sight.service

import com.sight.controllers.http.dto.AnswerDto
import com.sight.controllers.http.dto.GetAnswersResponse
import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupCategory
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.MatchedGroupRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class GroupMatchingAnswerService(
    private val answerRepository: GroupMatchingAnswerRepository,
    private val answerFieldRepository: GroupMatchingAnswerFieldRepository,
    private val subjectRepository: GroupMatchingSubjectRepository,
    private val matchedGroupRepository: MatchedGroupRepository,
    private val fieldRepository: GroupMatchingFieldRepository,
) {
    companion object {
        private const val DEFAULT_OFFSET = 0
        private const val DEFAULT_LIMIT = 20
    }

    fun getAllAnswers(
        groupMatchingId: String,
        groupType: String? = null,
        fieldId: String? = null,
        offset: Int = DEFAULT_OFFSET,
        limit: Int = DEFAULT_LIMIT,
    ): GetAnswersResponse {
        // groupType 검증 - STUDY와 PROJECT만 허용
        val groupTypeEnum: GroupCategory? =
            groupType?.let {
                when (it.uppercase()) {
                    "STUDY" -> GroupCategory.STUDY
                    "PROJECT" -> GroupCategory.PROJECT
                    else -> throw BadRequestException("유효하지 않은 그룹 타입입니다")
                }
            }

        // fieldId 검증
        if (fieldId != null && !fieldRepository.existsById(fieldId)) {
            throw BadRequestException("유효하지 않은 fieldId입니다")
        }

        // offset/limit 검증
        if (offset < 0) {
            throw BadRequestException("offset은 0 이상이어야 합니다")
        }
        if (limit <= 0) {
            throw BadRequestException("limit은 양의 정수여야 합니다")
        }

        // Pageable 생성 (offset 기반)
        val pageNumber = offset / limit
        val pageable = PageRequest.of(pageNumber, limit)

        // DB 쿼리로 필터링 및 페이지네이션
        val page = answerRepository.findAnswersWithFilters(groupMatchingId, groupTypeEnum, fieldId, pageable)

        // DTO 변환
        val answerDtos =
            page.content.map { answer ->
                AnswerDto(
                    answerId = answer.id,
                    answerUserId = answer.userId,
                    createdAt = answer.createdAt,
                    updatedAt = answer.updatedAt,
                    groupType = answer.groupType,
                    isPreferOnline = answer.isPreferOnline,
                    selectedFields = getSelectedFields(answer.id),
                    subjectIdeas = getSubjectIdeas(answer.id),
                    matchedGroupIds = getMatchedGroupIds(answer.id),
                )
            }

        return GetAnswersResponse(
            answers = answerDtos,
            total = page.totalElements.toInt(),
        )
    }

    private fun getSelectedFields(answerId: String): List<String> {
        val fields =
            answerFieldRepository.findAllByAnswerId(answerId)
                .map { it.fieldId }
        if (fields.isEmpty()) {
            throw BadRequestException("선택한 관심분야가 존재하지 않습니다")
        }
        return fields
    }

    private fun getSubjectIdeas(answerId: String): List<String> {
        return subjectRepository.findAllByAnswerId(answerId)
            .map { it.subject }
    }

    private fun getMatchedGroupIds(answerId: String): List<Long> {
        return matchedGroupRepository.findAllByAnswerId(answerId)
            .map { it.groupId }
    }
}
