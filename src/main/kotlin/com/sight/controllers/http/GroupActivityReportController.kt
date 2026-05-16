package com.sight.controllers.http

import com.sight.controllers.http.dto.ActivityReportResponse
import com.sight.controllers.http.dto.EditActivityReportRequest
import com.sight.controllers.http.dto.GetUploadLinkResponse
import com.sight.controllers.http.dto.ListActivityReportResponse
import com.sight.controllers.http.dto.ListActivityReportsResponse
import com.sight.controllers.http.dto.SubmitActivityReportRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupActivityReportService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupActivityReportController(
    private val groupActivityReportService: GroupActivityReportService,
) {
    companion object {
        fun uploadLinkRequestPath(groupId: Long) = "/groups/$groupId/activity-report/upload-link"
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/groups/{groupId}/activity-report/upload-link")
    fun getUploadLink(
        @PathVariable groupId: Long,
        requester: Requester,
    ): GetUploadLinkResponse {
        val result = groupActivityReportService.getUploadLink(groupId, requester.userId, requestPath = uploadLinkRequestPath(groupId))
        return GetUploadLinkResponse(
            url = result.url,
            fileKey = result.fileKey,
            fileUploadId = result.fileUploadId,
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/groups/{groupId}/activity-report")
    @ResponseStatus(HttpStatus.CREATED)
    fun submitReport(
        @PathVariable groupId: Long,
        @Valid @RequestBody request: SubmitActivityReportRequest,
        requester: Requester,
    ): ActivityReportResponse {
        val result =
            groupActivityReportService.submitReport(
                groupId = groupId,
                requesterId = requester.userId,
                isPresentation = request.isPresentation!!,
                fileUploadId = request.fileUploadId!!,
                uploadLinkRequestPath = uploadLinkRequestPath(groupId),
            )
        return result.toResponse()
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PatchMapping("/groups/{groupId}/activity-report/{reportId}")
    fun editReport(
        @PathVariable groupId: Long,
        @PathVariable reportId: String,
        @RequestBody request: EditActivityReportRequest,
        requester: Requester,
    ): ActivityReportResponse {
        val result =
            groupActivityReportService.editReport(
                groupId = groupId,
                reportId = reportId,
                requesterId = requester.userId,
                isPresentation = request.isPresentation,
                fileUploadId = request.fileUploadId,
                uploadLinkRequestPath = uploadLinkRequestPath(groupId),
            )
        return result.toResponse()
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @DeleteMapping("/groups/{groupId}/activity-report/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancelReport(
        @PathVariable groupId: Long,
        @PathVariable reportId: String,
        requester: Requester,
    ) {
        groupActivityReportService.cancelReport(
            groupId = groupId,
            reportId = reportId,
            requesterId = requester.userId,
            isManager = requester.role == UserRole.MANAGER,
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/groups/{groupId}/activity-report")
    fun listReports(
        @PathVariable groupId: Long,
        requester: Requester,
    ): ListActivityReportsResponse {
        val result =
            groupActivityReportService.listReports(
                groupId = groupId,
                requesterId = requester.userId,
                isManager = requester.role == UserRole.MANAGER,
            )
        return ListActivityReportsResponse(
            reports =
                result.reports.map { item ->
                    ListActivityReportResponse(
                        id = item.id,
                        groupId = item.groupId,
                        seminarDate = item.seminarDate,
                        seminarIsSummerSeason = item.seminarIsSummerSeason,
                        seminarIsSpeakAfter = item.seminarIsSpeakAfter,
                        isPresentation = item.isPresentation,
                        reportFileUrl = item.reportFileUrl,
                        createdAt = item.createdAt,
                        updatedAt = item.updatedAt,
                    )
                },
        )
    }

    private fun com.sight.service.ActivityReportResult.toResponse(): ActivityReportResponse =
        ActivityReportResponse(
            id = id,
            groupId = groupId,
            seminarId = seminarId,
            isPresentation = isPresentation,
            reportFileKey = reportFileKey,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
