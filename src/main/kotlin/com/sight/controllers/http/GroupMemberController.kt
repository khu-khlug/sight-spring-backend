package com.sight.controllers.http

import com.sight.controllers.http.dto.ListGroupMemberResponse
import com.sight.controllers.http.dto.ListGroupMembersResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupMemberService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupMemberController(
    private val groupMemberService: GroupMemberService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/groups/{groupId}/members")
    fun listGroupMembers(
        @PathVariable groupId: Long,
        requester: Requester,
    ): ListGroupMembersResponse {
        val result =
            groupMemberService.listGroupMembers(
                groupId = groupId,
                requesterId = requester.userId,
            )

        return ListGroupMembersResponse(
            members =
                result.members.map { member ->
                    ListGroupMemberResponse(
                        userId = member.userId,
                        name = member.name,
                        realname = member.realname,
                        isLeader = member.isLeader,
                    )
                },
        )
    }
}
