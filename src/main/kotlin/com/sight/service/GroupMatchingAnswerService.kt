package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
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
import com.sight.service.dto.GroupMatchingAnswerResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupMatchingAnswerService(
    private val groupMatchingAnswerRepository: GroupMatchingAnswerRepository,
    private val groupMatchingAnswerFieldRepository: GroupMatchingAnswerFieldRepository,
    private val groupMatchingFieldRepository: GroupMatchingFieldRepository,
    private val groupMatchingSubjectRepository: GroupMatchingSubjectRepository,
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

        if (groupMatchingAnswerRepository.existsByUserIdAndGroupMatchingId(userId, groupMatchingId)) {
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
        val savedAnswer = groupMatchingAnswerRepository.save(fieldRequest)
        //  필드 선택지 저장
        val fieldEntities =
            groupMatchingFieldIds.map { fieldId ->

                if (!groupMatchingFieldRepository.existsById(fieldId)) {
                    throw UnprocessableEntityException("유효하지 않은 필드 선택지 ID: $fieldId")
                } else {
                    GroupMatchingAnswerField(
                        id = UlidCreator.getUlid().toString(),
                        answerId = savedAnswer.id,
                        fieldId = fieldId,
                    )
                }
            }
        groupMatchingAnswerFieldRepository.saveAll(fieldEntities)
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
            val savedSubjectEntities = groupMatchingSubjectRepository.saveAll(subjectEntities)
            createdSubjectIds = savedSubjectEntities.map { it.id }
        }

        return GroupMatchingAnswerResult(
            answer = savedAnswer,
            fieldIds = groupMatchingFieldIds,
            subjectIds = createdSubjectIds,
        )
    }
}
