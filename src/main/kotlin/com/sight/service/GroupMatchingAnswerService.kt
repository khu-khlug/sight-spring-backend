package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingAnswerField
import com.sight.domain.groupmatching.GroupMatchingSubject
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.service.dto.AnswerSummary
import com.sight.service.dto.GroupMatchingAnswerResult
import com.sight.service.dto.ListAnswersResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupMatchingAnswerService(
    private val answerRepository: GroupMatchingAnswerRepository,
    private val answerFieldRepository: GroupMatchingAnswerFieldRepository,
    private val subjectRepository: GroupMatchingSubjectRepository,
    private val matchedGroupRepository: MatchedGroupRepository,
    private val fieldRepository: GroupMatchingFieldRepository,
    private val groupMatchingRepository: GroupMatchingRepository,
) {
    @Transactional
    fun createGroupMatchingAnswer(
        groupType: GroupCategory,
        isPreferOnline: Boolean,
        userId: Long,
        groupMatchingId: String,
        groupMatchingFieldIds: List<String>,
        groupMatchingSubjects: List<String>,
    ): GroupMatchingAnswerResult {
        // [Logic Added] 1. 중복 제출 검증
        val groupMatching =
            groupMatchingRepository.findById(groupMatchingId)
                .orElseThrow { NotFoundException("해당 그룹 매칭을 찾을 수 없습니다.") }

        val now = LocalDateTime.now()

        if (groupMatching.closedAt.isBefore(now) || groupMatching.closedAt.isEqual(now)) {
            throw UnprocessableEntityException("그룹 매칭 응답 기간이 마감되었습니다. (마감일시: ${groupMatching.closedAt})")
        }

        if (answerRepository.existsByUserIdAndGroupMatchingId(userId, groupMatchingId)) {
            throw UnprocessableEntityException("이미 응답을 제출했습니다.")
        }
        val fieldRequest =
            GroupMatchingAnswer(
                id = UlidCreator.getUlid().toString(),
                userId = userId,
                groupType = groupType,
                isPreferOnline = isPreferOnline,
                groupMatchingId = groupMatchingId,
            )
        val savedAnswer = answerRepository.save(fieldRequest)
        //  필드 선택지 저장
        val foundFields = fieldRepository.findAllById(groupMatchingFieldIds)
        val validFieldIds = foundFields.map { it.id }.toSet()

        val fieldEntities =
            groupMatchingFieldIds.map { fieldId ->
                if (!validFieldIds.contains(fieldId)) {
                    throw UnprocessableEntityException("유효하지 않은 필드 선택지 ID: $fieldId")
                }

                GroupMatchingAnswerField(
                    id = UlidCreator.getUlid().toString(),
                    answerId = savedAnswer.id,
                    fieldId = fieldId,
                )
            }
        answerFieldRepository.saveAll(fieldEntities)
        //  답변 엔티티 생성 및 저장

        //  주제 저장
        var createdSubjectIds: List<String> = emptyList()
        if (groupMatchingSubjects.isNotEmpty()) {
            val subjectEntities =
                groupMatchingSubjects.map { subjectString ->
                    GroupMatchingSubject(
                        id = UlidCreator.getUlid().toString(),
                        answerId = savedAnswer.id,
                        subject = subjectString,
                    )
                }
            val savedSubjectEntities = subjectRepository.saveAll(subjectEntities)
            createdSubjectIds = savedSubjectEntities.map { it.id }
        }

        return GroupMatchingAnswerResult(
            answer = savedAnswer,
            fieldIds = groupMatchingFieldIds,
            subjectIds = createdSubjectIds,
        )
    }

    fun listAnswers(
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
                    selectedFields = getSelectedFieldIds(answer.id),
                    subjectIdeas = getSubjectIdeas(answer.id),
                    matchedGroupIds = getMatchedGroupIds(answer.id),
                )
            }

        return ListAnswersResult(
            answers = answerSummaries,
            total = page.totalElements.toInt(),
        )
    }

    private fun getSelectedFieldIds(answerId: String): List<String> {
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
