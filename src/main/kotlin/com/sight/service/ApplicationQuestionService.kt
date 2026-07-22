package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
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
}
