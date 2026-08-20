package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateSupportRequestCommentRequest
import com.sight.controllers.http.dto.CreateSupportRequestCommentResponse
import com.sight.controllers.http.dto.CreateSupportRequestRequest
import com.sight.controllers.http.dto.CreateSupportRequestResponse
import com.sight.controllers.http.dto.GetSupportRequestResponse
import com.sight.controllers.http.dto.ListSupportRequestResponse
import com.sight.controllers.http.dto.ListSupportRequestsResponse
import com.sight.controllers.http.dto.SupportRequestCommentResponse
import com.sight.controllers.http.dto.SupportRequestUserResponse
import com.sight.controllers.http.dto.UpdateSupportRequestRequest
import com.sight.controllers.http.dto.UpdateSupportRequestResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.SupportRequestCommentResult
import com.sight.service.SupportRequestDetail
import com.sight.service.SupportRequestService
import com.sight.service.SupportRequestSummary
import com.sight.service.SupportRequestUser
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class SupportRequestController(
    private val supportRequestService: SupportRequestService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/support-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSupportRequest(
        requester: Requester,
        @Valid @RequestBody request: CreateSupportRequestRequest,
    ): CreateSupportRequestResponse =
        supportRequestService
            .createSupportRequest(
                requesterId = requester.userId,
                category = checkNotNull(request.category),
                title = checkNotNull(request.title),
                content = checkNotNull(request.content),
            ).toCreateResponse()

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/support-requests")
    fun listSupportRequests(
        @RequestParam(defaultValue = "0") @Min(0) offset: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) limit: Int,
        @RequestParam(required = false)
        @Pattern(regexp = "SERVER_SPACE|SUBDOMAIN|HARDWARE|BOOK|OTHER")
        category: String?,
    ): ListSupportRequestsResponse {
        val result = supportRequestService.listSupportRequests(offset, limit, category)
        return ListSupportRequestsResponse(
            count = result.count,
            supportRequests = result.supportRequests.map { it.toListResponse() },
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/support-requests/{supportRequestId}")
    fun getSupportRequest(
        @PathVariable supportRequestId: String,
    ): GetSupportRequestResponse = supportRequestService.getSupportRequestById(supportRequestId).toGetResponse()

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PutMapping("/support-requests/{supportRequestId}")
    fun updateSupportRequest(
        @PathVariable supportRequestId: String,
        requester: Requester,
        @Valid @RequestBody request: UpdateSupportRequestRequest,
    ): UpdateSupportRequestResponse =
        supportRequestService
            .updateSupportRequest(
                supportRequestId = supportRequestId,
                requesterId = requester.userId,
                category = checkNotNull(request.category),
                title = checkNotNull(request.title),
                content = checkNotNull(request.content),
            ).toUpdateResponse()

    @Auth([UserRole.MANAGER])
    @DeleteMapping("/support-requests/{supportRequestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteSupportRequest(
        @PathVariable supportRequestId: String,
    ) {
        supportRequestService.deleteSupportRequest(supportRequestId)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/support-requests/{supportRequestId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSupportRequestComment(
        @PathVariable supportRequestId: String,
        requester: Requester,
        @Valid @RequestBody request: CreateSupportRequestCommentRequest,
    ): CreateSupportRequestCommentResponse =
        supportRequestService
            .createSupportRequestComment(
                supportRequestId = supportRequestId,
                authorId = requester.userId,
                isManager = requester.role == UserRole.MANAGER,
                content = checkNotNull(request.content),
            ).toCreateCommentResponse()

    private fun SupportRequestSummary.toListResponse(): ListSupportRequestResponse =
        ListSupportRequestResponse(
            id = supportRequest.id,
            category = supportRequest.category,
            title = supportRequest.title,
            content = supportRequest.content,
            requester = requester.toResponse(),
            hasComments = hasComments,
            createdAt = supportRequest.createdAt,
            updatedAt = supportRequest.updatedAt,
        )

    private fun SupportRequestSummary.toCreateResponse(): CreateSupportRequestResponse =
        CreateSupportRequestResponse(
            id = supportRequest.id,
            category = supportRequest.category,
            title = supportRequest.title,
            content = supportRequest.content,
            requester = requester.toResponse(),
            hasComments = hasComments,
            createdAt = supportRequest.createdAt,
            updatedAt = supportRequest.updatedAt,
        )

    private fun SupportRequestSummary.toUpdateResponse(): UpdateSupportRequestResponse =
        UpdateSupportRequestResponse(
            id = supportRequest.id,
            category = supportRequest.category,
            title = supportRequest.title,
            content = supportRequest.content,
            requester = requester.toResponse(),
            hasComments = hasComments,
            createdAt = supportRequest.createdAt,
            updatedAt = supportRequest.updatedAt,
        )

    private fun SupportRequestDetail.toGetResponse(): GetSupportRequestResponse =
        GetSupportRequestResponse(
            id = supportRequest.id,
            category = supportRequest.category,
            title = supportRequest.title,
            content = supportRequest.content,
            requester = requester.toResponse(),
            hasComments = comments.isNotEmpty(),
            createdAt = supportRequest.createdAt,
            updatedAt = supportRequest.updatedAt,
            comments = comments.map { it.toResponse() },
        )

    private fun SupportRequestCommentResult.toCreateCommentResponse(): CreateSupportRequestCommentResponse =
        CreateSupportRequestCommentResponse(
            id = comment.id,
            content = comment.content,
            author = author.toResponse(),
            createdAt = comment.createdAt,
        )

    private fun SupportRequestCommentResult.toResponse(): SupportRequestCommentResponse =
        SupportRequestCommentResponse(
            id = comment.id,
            content = comment.content,
            author = author.toResponse(),
            createdAt = comment.createdAt,
        )

    private fun SupportRequestUser.toResponse(): SupportRequestUserResponse = SupportRequestUserResponse(userId = userId, name = name)
}
