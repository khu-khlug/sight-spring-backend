package com.sight.controllers.http

import com.sight.controllers.http.dto.AssignApplicationFormManagerRequest
import com.sight.controllers.http.dto.CreateApplicationFormDraftRequest
import com.sight.controllers.http.dto.CreateApplicationFormDraftResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.ApplicationFormService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ApplicationFormController(
    private val applicationFormService: ApplicationFormService,
) {
    @PostMapping("/application-forms")
    @ResponseStatus(HttpStatus.CREATED)
    fun createDraft(
        @Valid @RequestBody request: CreateApplicationFormDraftRequest,
    ): CreateApplicationFormDraftResponse {
        val draft =
            applicationFormService.createDraft(
                info21Id = request.info21Id,
                info21Password = request.info21Password,
            )

        return CreateApplicationFormDraftResponse(
            id = draft.id,
            info21Id = draft.info21Id,
            submittee = draft.submittee,
            token = draft.token,
            status = draft.status,
            interviewAvailableTimes =
                draft.interviewAvailableTimes.map { availableTime ->
                    CreateApplicationFormDraftResponse.InterviewAvailableTimeResponse(
                        id = availableTime.id,
                        availableAt = availableTime.availableAt,
                        createdAt = availableTime.createdAt,
                    )
                },
            contents =
                draft.contents.map { content ->
                    CreateApplicationFormDraftResponse.ApplicationContentResponse(
                        id = content.id,
                        questionId = content.questionId,
                        content = content.content,
                        createdAt = content.createdAt,
                        updatedAt = content.updatedAt,
                    )
                },
            createdAt = draft.createdAt,
            updatedAt = draft.updatedAt,
        )
    }

    @Auth([UserRole.MANAGER])
    @PutMapping("/application-forms/{applicationFormId}/assignee")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun assignManager(
        @PathVariable applicationFormId: String,
        @Valid @RequestBody request: AssignApplicationFormManagerRequest,
    ) {
        applicationFormService.assignManager(
            applicationFormId = applicationFormId,
            managerUserId = request.managerUserId!!,
        )
    }
}
