package com.sight.controllers.http

import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupFileDownloadService
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupFileController(private val files: GroupFileDownloadService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.VIEWER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/files/{fileId}")
    fun getGroupFile(
        @PathVariable @Positive(message = "그룹 아이디는 양의 정수여야 합니다.") groupId: Long,
        @PathVariable fileId: String,
        requester: Requester,
    ): ResponseEntity<Void> {
        val url = files.getGroupFileUrl(groupId, fileId)
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, url)
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .build()
    }
}
