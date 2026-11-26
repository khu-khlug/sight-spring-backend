package com.sight.controllers.http

import com.sight.controllers.http.dto.ActivityReportResponse
import com.sight.controllers.http.dto.CreateActivityReportUploadLinkRequest
import com.sight.controllers.http.dto.EditActivityReportRequest
import com.sight.controllers.http.dto.GetUploadLinkResponse
import com.sight.controllers.http.dto.ListActivityReportsResponse
import com.sight.controllers.http.dto.SubmitActivityReportRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupActivityReportService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
import org.springframework.web.util.UriComponentsBuilder

@RestController
class GroupActivityReportController(
    private val groupActivityReportService: GroupActivityReportService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.LEADER], groupId = "groupId", allowManager = false)
    @PostMapping("/groups/{groupId}/activity-reports/upload-link")
    @ResponseStatus(HttpStatus.OK)
    fun getUploadLink(
        @PathVariable groupId: Long,
        @RequestBody request: CreateActivityReportUploadLinkRequest,
        requester: Requester,
    ): GetUploadLinkResponse {
        val result =
            groupActivityReportService.getUploadLink(
                groupId,
                requester.userId,
                request.size,
                uploadLinkRequestPath(groupId),
            )
        return GetUploadLinkResponse(
            url = result.url,
            fileKey = result.fileKey,
            fileUploadId = result.fileUploadId,
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.LEADER], groupId = "groupId", allowManager = false)
    @PostMapping("/groups/{groupId}/activity-reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun submitReport(
        @PathVariable groupId: Long,
        @RequestBody request: SubmitActivityReportRequest,
        requester: Requester,
    ): ActivityReportResponse {
        val result =
            groupActivityReportService.submitReport(
                groupId = groupId,
                requesterId = requester.userId,
                isPresentation = request.isPresentation,
                fileUploadId = request.fileUploadId,
                uploadLinkRequestPath = uploadLinkRequestPath(groupId),
            )
        return ActivityReportResponse.from(result)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.LEADER], groupId = "groupId", allowManager = false)
    @PatchMapping("/groups/{groupId}/activity-reports/{reportId}")
    @ResponseStatus(HttpStatus.OK)
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
        return ActivityReportResponse.from(result)
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.LEADER], groupId = "groupId", allowManager = true)
    @DeleteMapping("/groups/{groupId}/activity-reports/{reportId}")
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
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.MEMBER], groupId = "groupId", allowManager = true)
    @GetMapping("/groups/{groupId}/activity-reports")
    @ResponseStatus(HttpStatus.OK)
    fun listReports(
        @PathVariable groupId: Long,
    ): ListActivityReportsResponse = ListActivityReportsResponse.from(groupActivityReportService.listReports(groupId))

    companion object {
        private fun uploadLinkRequestPath(groupId: Long): String =
            MvcUriComponentsBuilder.fromMethodName(
                UriComponentsBuilder.fromPath(""),
                GroupActivityReportController::class.java,
                GroupActivityReportController::getUploadLink.name,
                groupId,
                null,
                null,
            ).build().toUriString()
    }
}
