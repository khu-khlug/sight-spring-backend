package com.sight.controllers.http

import com.sight.controllers.http.dto.ListGroupMemberResponse
import com.sight.controllers.http.dto.ListGroupMembersResponse
import com.sight.controllers.http.dto.UpdateGroupMasterRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupMemberService
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

    @Auth([UserRole.USER, UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/groups/{groupId}/master")
    fun delegateGroupMaster(
        @PathVariable groupId: Long,
        @Valid @RequestBody request: UpdateGroupMasterRequest,
        requester: Requester,
    ) {
        groupMemberService.delegateMaster(
            groupId = groupId,
            requesterId = requester.userId,
            newMasterId = request.memberId,
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/groups/{groupId}/members/@me")
    fun joinGroup(
        @PathVariable groupId: Long,
        requester: Requester,
    ) {
        groupMemberService.joinGroup(
            groupId = groupId,
            requesterId = requester.userId,
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    fun kickGroupMember(
        @PathVariable groupId: Long,
        @PathVariable memberId: Long,
        requester: Requester,
    ) {
        groupMemberService.kickMember(
            groupId = groupId,
            requesterId = requester.userId,
            kickedMemberId = memberId,
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/groups/{groupId}/members/@me")
    fun leaveGroup(
        @PathVariable groupId: Long,
        requester: Requester,
    ) {
        groupMemberService.leaveGroup(
            groupId = groupId,
            requesterId = requester.userId,
        )
    }
}
