package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupCategory
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.service.dto.AnswerSummary
import com.sight.service.dto.ListAnswersResult
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
    fun getAllAnswers(
        groupMatchingId: String,
        groupType: GroupCategory? = null,
        fieldId: String? = null,
        offset: Int,
        limit: Int,
    ): ListAnswersResult {
        // fieldId 검증 - 비즈니스 로직 검증 (존재 여부 및 obsoleted 체크)
        if (fieldId != null) {
            val field =
                fieldRepository.findById(fieldId)
                    .orElseThrow { BadRequestException("유효하지 않은 관심분야입니다") }

            if (field.obsoletedAt != null) {
                throw BadRequestException("유효하지 않은 관심분야입니다")
            }
        }

        // Pageable 생성 (offset 기반)
        val pageNumber = offset / limit
        val pageable = PageRequest.of(pageNumber, limit)

        // DB 쿼리로 필터링 및 페이지네이션
        val page = answerRepository.findAnswersWithFilters(groupMatchingId, groupType, fieldId, pageable)

        // DTO 변환
        val answerSummaries =
            page.content.map { answer ->
                AnswerSummary(
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

        return ListAnswersResult(
            answers = answerSummaries,
            total = page.totalElements.toInt(),
        )
    }

    private fun getSelectedFields(answerId: String): List<String> {
        val fieldIds =
            answerFieldRepository.findAllByAnswerId(answerId)
                .map { it.fieldId }
        if (fieldIds.isEmpty()) {
            throw BadRequestException("선택한 관심분야가 존재하지 않습니다")
        }
        return fieldIds
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
