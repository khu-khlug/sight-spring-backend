package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateApplicationCommentRequest
import com.sight.controllers.http.dto.CreateApplicationCommentResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.ApplicationFormService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ApplicationFormController(
    private val applicationFormService: ApplicationFormService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/application-forms/{applicationFormId}/comments")
    fun createComment(
        @PathVariable applicationFormId: String,
        requester: Requester,
        @Valid @RequestBody request: CreateApplicationCommentRequest,
    ): CreateApplicationCommentResponse {
        val comment =
            applicationFormService.createComment(
                applicationFormId = applicationFormId,
                authorUserId = requester.userId,
                content = request.content,
            )

        return CreateApplicationCommentResponse(
            id = comment.id,
            applicationFormId = comment.applicationFormId,
            authorUserId = comment.authorUserId,
            content = comment.content,
            createdAt = comment.createdAt,
            updatedAt = comment.updatedAt,
        )
    }

    @Auth([UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/application-forms/{applicationFormId}/pass")
    fun passApplicationForm(
        @PathVariable applicationFormId: String,
        requester: Requester,
    ): ResponseEntity<Void> {
        applicationFormService.passApplicationForm(
            applicationFormId = applicationFormId,
            authorUserId = requester.userId,
        )
        return ResponseEntity.noContent().build()
    }

    @Auth([UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/application-forms/{applicationFormId}/reject")
    fun rejectApplicationForm(
        @PathVariable applicationFormId: String,
        requester: Requester,
    ): ResponseEntity<Void> {
        applicationFormService.rejectApplicationForm(
            applicationFormId = applicationFormId,
            authorUserId = requester.userId,
        )
        return ResponseEntity.noContent().build()
    }
}
