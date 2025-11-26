package com.sight.service

import com.sight.controllers.http.dto.AnswerDto
import com.sight.controllers.http.dto.GetAnswersResponse
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.MatchedGroupRepository
import org.springframework.stereotype.Service

@Service
class GroupMatchingAnswerService(
    private val answerRepository: GroupMatchingAnswerRepository,
    private val answerFieldRepository: GroupMatchingAnswerFieldRepository,
    private val subjectRepository: GroupMatchingSubjectRepository,
    private val matchedGroupRepository: MatchedGroupRepository,
) {
    fun getAllAnswers(groupMatchingId: String): GetAnswersResponse {
        val answers = answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId)

        val answerDtos =
            answers.map { answer ->
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
            total = answerDtos.size,
        )
    }

    private fun getSelectedFields(answerId: String): List<String> {
        return answerFieldRepository.findAllByAnswerId(answerId)
            .map { it.fieldId }
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
