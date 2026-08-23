package com.sight.controllers.http.dto

import com.sight.domain.supportrequest.SupportRequestCategory
import com.sight.service.SupportRequestCommentResult
import com.sight.service.SupportRequestDetail
import com.sight.service.SupportRequestSummary
import com.sight.service.SupportRequestUser
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class SupportRequestUserResponse(
    val userId: Long,
    val name: String,
) {
    companion object {
        fun from(user: SupportRequestUser): SupportRequestUserResponse = SupportRequestUserResponse(user.userId, user.name)
    }
}

data class SupportRequestCommentResponse(
    val id: String,
    val content: String,
    val author: SupportRequestUserResponse,
    val createdAt: Instant,
) {
    companion object {
        fun from(commentResult: SupportRequestCommentResult): SupportRequestCommentResponse =
            SupportRequestCommentResponse(
                id = commentResult.comment.id,
                content = commentResult.comment.content,
                author = SupportRequestUserResponse.from(commentResult.author),
                createdAt = commentResult.comment.createdAt,
            )
    }
}

data class ListSupportRequestResponse(
    val id: String,
    val category: SupportRequestCategory,
    val title: String,
    val content: String,
    val requester: SupportRequestUserResponse,
    val hasComments: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(summary: SupportRequestSummary): ListSupportRequestResponse =
            ListSupportRequestResponse(
                id = summary.supportRequest.id,
                category = summary.supportRequest.category,
                title = summary.supportRequest.title,
                content = summary.supportRequest.content,
                requester = SupportRequestUserResponse.from(summary.requester),
                hasComments = summary.hasComments,
                createdAt = summary.supportRequest.createdAt,
                updatedAt = summary.supportRequest.updatedAt,
            )
    }
}

data class ListSupportRequestsResponse(
    val count: Long,
    val supportRequests: List<ListSupportRequestResponse>,
)

data class GetSupportRequestResponse(
    val id: String,
    val category: SupportRequestCategory,
    val title: String,
    val content: String,
    val requester: SupportRequestUserResponse,
    val hasComments: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val comments: List<SupportRequestCommentResponse>,
) {
    companion object {
        fun from(detail: SupportRequestDetail): GetSupportRequestResponse =
            GetSupportRequestResponse(
                id = detail.supportRequest.id,
                category = detail.supportRequest.category,
                title = detail.supportRequest.title,
                content = detail.supportRequest.content,
                requester = SupportRequestUserResponse.from(detail.requester),
                hasComments = detail.comments.isNotEmpty(),
                createdAt = detail.supportRequest.createdAt,
                updatedAt = detail.supportRequest.updatedAt,
                comments = detail.comments.map(SupportRequestCommentResponse::from),
            )
    }
}

data class CreateSupportRequestRequest(
    @field:NotNull
    val category: SupportRequestCategory?,
    @field:NotBlank
    @field:Size(max = 255)
    val title: String?,
    @field:NotBlank
    val content: String?,
)

data class CreateSupportRequestResponse(
    val id: String,
    val category: SupportRequestCategory,
    val title: String,
    val content: String,
    val requester: SupportRequestUserResponse,
    val hasComments: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(summary: SupportRequestSummary): CreateSupportRequestResponse =
            CreateSupportRequestResponse(
                id = summary.supportRequest.id,
                category = summary.supportRequest.category,
                title = summary.supportRequest.title,
                content = summary.supportRequest.content,
                requester = SupportRequestUserResponse.from(summary.requester),
                hasComments = summary.hasComments,
                createdAt = summary.supportRequest.createdAt,
                updatedAt = summary.supportRequest.updatedAt,
            )
    }
}

data class UpdateSupportRequestRequest(
    @field:NotNull
    val category: SupportRequestCategory?,
    @field:NotBlank
    @field:Size(max = 255)
    val title: String?,
    @field:NotBlank
    val content: String?,
)

data class UpdateSupportRequestResponse(
    val id: String,
    val category: SupportRequestCategory,
    val title: String,
    val content: String,
    val requester: SupportRequestUserResponse,
    val hasComments: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(summary: SupportRequestSummary): UpdateSupportRequestResponse =
            UpdateSupportRequestResponse(
                id = summary.supportRequest.id,
                category = summary.supportRequest.category,
                title = summary.supportRequest.title,
                content = summary.supportRequest.content,
                requester = SupportRequestUserResponse.from(summary.requester),
                hasComments = summary.hasComments,
                createdAt = summary.supportRequest.createdAt,
                updatedAt = summary.supportRequest.updatedAt,
            )
    }
}

data class CreateSupportRequestCommentRequest(
    @field:NotBlank
    val content: String?,
)

data class CreateSupportRequestCommentResponse(
    val id: String,
    val content: String,
    val author: SupportRequestUserResponse,
    val createdAt: Instant,
) {
    companion object {
        fun from(commentResult: SupportRequestCommentResult): CreateSupportRequestCommentResponse =
            CreateSupportRequestCommentResponse(
                id = commentResult.comment.id,
                content = commentResult.comment.content,
                author = SupportRequestUserResponse.from(commentResult.author),
                createdAt = commentResult.comment.createdAt,
            )
    }
}
