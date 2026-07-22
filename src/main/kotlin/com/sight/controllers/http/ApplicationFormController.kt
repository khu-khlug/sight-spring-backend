package com.sight.controllers.http

import com.sight.controllers.http.dto.AssignApplicationFormManagerRequest
import com.sight.controllers.http.dto.CreateApplicationCommentRequest
import com.sight.controllers.http.dto.CreateApplicationCommentResponse
import com.sight.controllers.http.dto.CreateApplicationFormDraftRequest
import com.sight.controllers.http.dto.CreateApplicationFormDraftResponse
import com.sight.controllers.http.dto.GetApplicationFormDetailResponse
import com.sight.controllers.http.dto.ListApplicationFormsResponse
import com.sight.controllers.http.dto.SaveApplicationFormDraftRequest
import com.sight.controllers.http.dto.SubmitApplicationFormRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.ApplicationFormService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ApplicationFormController(
    private val applicationFormService: ApplicationFormService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/manager/application-forms")
    fun listForms(
        @RequestParam(defaultValue = "1") page: Int,
    ): ListApplicationFormsResponse {
        val forms = applicationFormService.listForms(page)
        return ListApplicationFormsResponse(
            forms.content.map {
                ListApplicationFormsResponse.Application(it.id, it.submittee, it.status, it.assignedUserId, it.createdAt, it.updatedAt)
            },
            forms.totalElements,
            forms.totalPages,
        )
    }

    @Auth([UserRole.MANAGER])
    @GetMapping("/manager/application-forms/{applicationFormId}")
    fun getDetail(
        @PathVariable applicationFormId: String,
    ): GetApplicationFormDetailResponse {
        val detail = applicationFormService.getDetail(applicationFormId)
        return GetApplicationFormDetailResponse(
            detail.form.id,
            detail.form.submittee,
            detail.form.status,
            detail.form.assignedUserId,
            detail.contents.map {
                GetApplicationFormDetailResponse.Content(it.questionId, it.content)
            },
            detail.times.map {
                GetApplicationFormDetailResponse.Time(it.availableAt)
            },
            detail.comments.map {
                GetApplicationFormDetailResponse.Comment(
                    it.id,
                    it.authorUserId,
                    it.content,
                    it.createdAt,
                )
            },
        )
    }

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

    @PutMapping("/application-forms/{applicationFormId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun saveDraft(
        @PathVariable applicationFormId: String,
        @Valid @RequestBody request: SaveApplicationFormDraftRequest,
    ) {
        applicationFormService.saveDraft(
            applicationFormId,
            request.token,
            request.interviewAvailableTimes.map {
                it.date to it.time
            },
            request.contents.associate { it.questionId to it.content },
        )
    }

    @PostMapping("/application-forms/{applicationFormId}/submit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun submit(
        @PathVariable applicationFormId: String,
        @Valid @RequestBody request: SubmitApplicationFormRequest,
    ) {
        applicationFormService.submit(applicationFormId, request.token)
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

    @Auth([UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/application-forms/{applicationFormId}/suspend")
    fun suspendApplicationForm(
        @PathVariable applicationFormId: String,
        requester: Requester,
    ): ResponseEntity<Void> {
        applicationFormService.suspendApplicationForm(
            applicationFormId = applicationFormId,
            authorUserId = requester.userId,
        )
        return ResponseEntity.noContent().build()
    }
}
