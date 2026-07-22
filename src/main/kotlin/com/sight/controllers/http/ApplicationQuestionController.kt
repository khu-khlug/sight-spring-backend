package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateApplicationQuestionRequest
import com.sight.controllers.http.dto.CreateApplicationQuestionResponse
import com.sight.controllers.http.dto.ListApplicationQuestionsResponse
import com.sight.controllers.http.dto.UpdateApplicationQuestionsRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.ApplicationQuestionService
import com.sight.service.UpdateApplicationQuestionCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class ApplicationQuestionController(
    private val applicationQuestionService: ApplicationQuestionService,
) {
    @GetMapping("/application-questions")
    fun listQuestions(
        @RequestParam
        @Size(min = 1, max = 100)
        applicationQuestionIds: List<String>,
    ): ListApplicationQuestionsResponse {
        val questions = applicationQuestionService.listQuestionsByIds(applicationQuestionIds)

        return ListApplicationQuestionsResponse(
            questions =
                questions.map { question ->
                    ListApplicationQuestionsResponse.QuestionResponse(
                        id = question.id,
                        title = question.title,
                        description = question.description,
                        minLength = question.minLength,
                        order = question.order,
                    )
                },
        )
    }

    @Auth([UserRole.MANAGER])
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/manager/application-questions")
    fun createQuestion(
        @Valid @RequestBody request: CreateApplicationQuestionRequest,
    ): CreateApplicationQuestionResponse {
        val question =
            applicationQuestionService.createQuestion(
                title = request.title,
                description = request.description,
                minLength = request.minLength,
            )

        return CreateApplicationQuestionResponse(
            id = question.id,
            title = question.title,
            description = question.description,
            minLength = question.minLength,
            order = question.order,
            isExposed = question.isExposed,
            createdAt = question.createdAt,
            updatedAt = question.updatedAt,
        )
    }

    @Auth([UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/manager/application-questions")
    fun updateQuestions(
        @Valid @RequestBody request: UpdateApplicationQuestionsRequest,
    ) {
        applicationQuestionService.updateQuestions(
            request.questions.map { question ->
                UpdateApplicationQuestionCommand(
                    id = question.id,
                    title = question.title,
                    description = question.description,
                    minLength = question.minLength,
                    order = question.order,
                    isExposed = question.isExposed,
                )
            },
        )
    }
}
