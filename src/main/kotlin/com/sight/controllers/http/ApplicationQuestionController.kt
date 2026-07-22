package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateApplicationQuestionRequest
import com.sight.controllers.http.dto.CreateApplicationQuestionResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.ApplicationQuestionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ApplicationQuestionController(
    private val applicationQuestionService: ApplicationQuestionService,
) {
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
}
