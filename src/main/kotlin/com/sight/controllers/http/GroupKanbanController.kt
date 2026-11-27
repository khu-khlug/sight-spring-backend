package com.sight.controllers.http

import com.sight.controllers.http.dto.ListGroupKanbanListsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.core.id.IntegerId
import com.sight.service.GroupKanbanQueryService
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupKanbanController(private val kanban: GroupKanbanQueryService) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.VIEWER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/lists")
    @ResponseStatus(HttpStatus.OK)
    fun listGroupKanbanLists(
        @PathVariable("groupId") @IntegerId(message = "그룹 아이디는 양의 정수여야 합니다.") groupId: String,
        @RequestParam(defaultValue = "0") @Min(0, message = "페이지 번호는 0 이상이어야 합니다.") page: Long,
        requester: Requester,
    ): ListGroupKanbanListsResponse {
        val groupIdValue = groupId.toLong()
        return ListGroupKanbanListsResponse.from(groupIdValue, kanban.listGroupKanbanLists(groupIdValue, page))
    }
}
