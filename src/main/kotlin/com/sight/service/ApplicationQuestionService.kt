package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
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

    @Transactional
    fun updateQuestions(commands: List<UpdateApplicationQuestionCommand>) {
        validateQuestionOrders(commands)

        val commandsById = commands.associateBy { it.id }
        if (commandsById.size != commands.size) {
            throw BadRequestException("중복된 가입신청서 문항 ID가 포함되어 있습니다")
        }

        val questions = applicationQuestionRepository.findAllById(commandsById.keys)
        if (questions.size != commands.size) {
            throw NotFoundException("존재하지 않는 가입신청서 문항이 포함되어 있습니다")
        }

        questions.forEach { question ->
            val command = commandsById.getValue(question.id)
            question.update(
                title = command.title,
                description = command.description,
                minLength = command.minLength,
                order = command.order,
                isExposed = command.isExposed,
            )
        }
        applicationQuestionRepository.saveAll(questions)
    }

    private fun validateQuestionOrders(commands: List<UpdateApplicationQuestionCommand>) {
        val orders = commands.mapNotNull { it.order }.sorted()
        if (orders != (1..orders.size).toList()) {
            throw BadRequestException("노출된 가입신청서 문항의 순서는 1부터 연속되어야 합니다")
        }
        if (commands.any { it.isExposed != (it.order != null) }) {
            throw BadRequestException("노출 여부와 문항 순서는 함께 설정되어야 합니다")
        }
    }
}
