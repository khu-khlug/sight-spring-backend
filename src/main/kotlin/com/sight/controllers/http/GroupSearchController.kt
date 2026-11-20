package com.sight.controllers.http

import com.sight.controllers.http.dto.ListGroupSearchResultResponse
import com.sight.controllers.http.dto.ListGroupSearchResultsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.core.groupauth.GroupAuth
import com.sight.core.groupauth.GroupPermission
import com.sight.service.GroupSearchService
import com.sight.service.GroupSearchTarget
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class GroupSearchController(
    private val groupSearchService: GroupSearchService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GroupAuth([GroupPermission.VIEWER], groupId = "groupId", allowManager = false)
    @GetMapping("/groups/{groupId}/search")
    fun listGroupSearchResults(
        @PathVariable groupId: Long,
        @RequestParam target: GroupSearchTarget,
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다") page: Int,
    ): ListGroupSearchResultsResponse {
        val result =
            groupSearchService.listGroupSearchResults(
                groupId = groupId,
                target = target,
                keyword = keyword,
                page = page,
            )

        return ListGroupSearchResultsResponse(
            page = result.page,
            totalPages = result.totalPages,
            receivedCount = result.results.size,
            results =
                result.results.map {
                    ListGroupSearchResultResponse(
                        listId = it.listId.toString(),
                        listTitle = it.listTitle,
                        cardId = it.cardId.toString(),
                        cardTitle = it.cardTitle,
                        recordId = it.recordId?.toString(),
                        matchedText = it.matchedText,
                    )
                },
        )
    }
}
