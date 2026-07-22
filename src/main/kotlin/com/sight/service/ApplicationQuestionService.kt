package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.NotFoundException
import com.sight.domain.application.ApplicationQuestion
import com.sight.repository.ApplicationQuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApplicationQuestionService(
    private val applicationQuestionRepository: ApplicationQuestionRepository,
) {
    @Transactional
    fun createQuestion(
        title: String,
        description: String,
        minLength: Int,
    ): ApplicationQuestion {
        val question =
            ApplicationQuestion(
                id = UlidCreator.getUlid().toString(),
                title = title,
                description = description,
                minLength = minLength,
                order = null,
                isExposed = false,
            )

        return applicationQuestionRepository.save(question)
    }

    @Transactional(readOnly = true)
    fun listQuestionsByIds(applicationQuestionIds: List<String>): List<ApplicationQuestion> {
        val distinctIds = applicationQuestionIds.distinct()
        val questionsById = applicationQuestionRepository.findAllById(distinctIds).associateBy { it.id }
        if (questionsById.size != distinctIds.size) {
            throw NotFoundException("존재하지 않는 가입신청서 문항이 포함되어 있습니다")
        }

        return distinctIds.map { questionId -> questionsById.getValue(questionId) }
    }
}
