package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingAnswerField
import com.sight.domain.groupmatching.GroupMatchingSubject
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingSubjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class GroupMatchingAnswerResult(
    val answer: GroupMatchingAnswer,
    val fieldIds: List<String>,
    val subjectIds: List<String>,
)

@Service
class GroupMatchingAnswerService(
    private val groupMatchingAnswerRepository: GroupMatchingAnswerRepository,
    private val groupMatchingAnswerFieldRepository: GroupMatchingAnswerFieldRepository,
    private val groupMatchingSubjectRepository: GroupMatchingSubjectRepository,
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
        // Repository에 existsByUserIdAndGroupMatchingId 메소드가 필요하다고 가정합니다.
        if (groupMatchingAnswerRepository.existsByUserIdAndGroupMatchingId(userId, groupMatchingId)) {
            throw UnprocessableEntityException("이미 응답을 제출했습니다.")
        }

        // 2. 답변 엔티티 생성 및 저장
        val fieldRequest =
            GroupMatchingAnswer(
                id = UlidCreator.getUlid().toString(),
                userId = userId,
                groupType = groupType,
                isPreferOnline = isPreferOnline,
                groupMatchingId = groupMatchingId,
            )
        val savedAnswer = groupMatchingAnswerRepository.save(fieldRequest)

        // 3. 필드 선택지 저장
        val fieldEntities =
            groupMatchingFieldIds.map { fieldId ->
                GroupMatchingAnswerField(
                    id = UlidCreator.getUlid().toString(),
                    answerId = savedAnswer.id,
                    fieldId = fieldId,
                )
            }
        groupMatchingAnswerFieldRepository.saveAll(fieldEntities)

        // 4. 서브젝트 저장 (Optional)
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
