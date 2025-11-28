package com.sight.controllers.http

import com.sight.controllers.http.dto.GetGroupMatchingAnswerResponse
import com.sight.controllers.http.dto.GetGroupMatchingGroupsResponse
import com.sight.controllers.http.dto.GroupMatchingGroupMemberResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.domain.group.GroupCategory
import com.sight.service.GroupMatchingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupMatchingController(
    private val groupMatchingService: GroupMatchingService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/groups")
    fun getGroups(
        @PathVariable groupMatchingId: String,
        @RequestParam(required = false) groupType: GroupCategory?,
    ): List<GetGroupMatchingGroupsResponse> {
        return groupMatchingService.getGroups(groupMatchingId, groupType).map { groupDto ->
            GetGroupMatchingGroupsResponse(
                id = groupDto.id,
                title = groupDto.title,
                members =
                    groupDto.members.map { memberDto ->
                        GroupMatchingGroupMemberResponse(
                            id = memberDto.id,
                            userId = memberDto.userId,
                            name = memberDto.name,
                            number = memberDto.number,
                        )
                    },
                createdAt = groupDto.createdAt,
            )
        }
    }

    @Auth(roles = [UserRole.USER, UserRole.MANAGER])
    @GetMapping("/group-matchings/{groupMatchingId}/answers/@me")
    fun getAnswer(
        @PathVariable groupMatchingId: String,
        requester: Requester?,
    ): GetGroupMatchingAnswerResponse {
        return groupMatchingService.getAnswer(groupMatchingId, requester!!.userId)
    }
}
