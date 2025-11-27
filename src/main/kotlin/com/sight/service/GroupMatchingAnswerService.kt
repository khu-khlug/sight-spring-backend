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

        // offset/limit 검증
        if (offset < 0) {
            throw BadRequestException("offset은 0 이상이어야 합니다")
        }
        if (limit <= 0) {
            throw BadRequestException("limit은 양의 정수여야 합니다")
        }

        var answers = answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId)

        // fieldId 검증 및 필터링
        if (fieldId != null) {
            if (!fieldRepository.existsById(fieldId)) {
                throw BadRequestException("유효하지 않은 fieldId입니다")
            } else {
                answers =
                    answers.filter { answer ->
                        answerFieldRepository.findAllByAnswerId(answer.id)
                            .any { it.fieldId == fieldId }
                    }
            }
        }
        // groupType 검증 및 필터링
        if (groupTypeEnum != null) {
            answers = answers.filter { it.groupType == groupTypeEnum }
        }

        val total = answers.size

        // 페이지네이션
        // drop(offset): offset개 요소를 건너뜀 (offset >= size면 빈 리스트 반환)
        // take(limit): 최대 limit개 요소를 가져옴 (남은 요소가 limit보다 적으면 남은 것만 반환)
        // 결과: offset이 범위를 벗어나도 에러 없이 빈 리스트 반환
        val pagedAnswers =
            answers
                .drop(offset)
                .take(limit)

        // DTO 변환
        val answerDtos =
            pagedAnswers.map { answer ->
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
            total = total,
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
