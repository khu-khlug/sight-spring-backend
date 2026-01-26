package com.sight.controllers.http

import com.sight.controllers.http.dto.AddGroupDiscordChannelMemberRequest
import com.sight.controllers.http.dto.CreateGroupDiscordChannelResponse
import com.sight.controllers.http.dto.CreateGroupRequest
import com.sight.controllers.http.dto.CreateGroupResponse
import com.sight.controllers.http.dto.GroupResponse
import com.sight.controllers.http.dto.ListGroupsResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupDiscordChannelService
import com.sight.service.GroupService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupController(
    private val groupDiscordChannelService: GroupDiscordChannelService,
    private val groupMatchingService: com.sight.service.GroupMatchingService,
    private val groupService: GroupService,
) {
    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/groups")
    fun listGroups(
        @RequestParam(defaultValue = "0") @Min(0) offset: Int,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) limit: Int,
        @RequestParam(required = false) bookmarked: Boolean?,
        requester: Requester,
    ): ListGroupsResponse {
        val result =
            groupService.listGroups(
                offset = offset,
                limit = limit,
                bookmarked = bookmarked,
                requesterId = requester.userId,
            )

        return ListGroupsResponse(
            count = result.count,
            groups =
                result.groups.map { group ->
                    GroupResponse(
                        id = group.id,
                        category = group.category,
                        title = group.title,
                        state = group.state,
                        countMember = group.countMember,
                        allowJoin = group.allowJoin,
                        createdAt = group.createdAt,
                    )
                },
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/groups/{groupId}/discord-channel")
    fun createGroupDiscordChannel(
        @PathVariable groupId: Long,
        requester: Requester,
    ): CreateGroupDiscordChannelResponse {
        val groupDiscordChannel =
            groupDiscordChannelService.createDiscordChannel(
                groupId = groupId,
                requesterId = requester.userId,
            )

        return CreateGroupDiscordChannelResponse(
            id = groupDiscordChannel.id,
            groupId = groupDiscordChannel.groupId,
            discordChannelId = groupDiscordChannel.discordChannelId,
            createdAt = groupDiscordChannel.createdAt,
        )
    }

    @Auth([UserRole.USER, UserRole.MANAGER])
    @PostMapping("/groups/{groupId}/discord-channel/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun addGroupDiscordChannelMember(
        @PathVariable groupId: Long,
        @Valid @RequestBody request: AddGroupDiscordChannelMemberRequest,
        requester: Requester,
    ) {
        groupDiscordChannelService.addMemberToDiscordChannel(
            groupId = groupId,
            memberId = request.memberId!!,
            requesterId = requester.userId,
        )
    }

    @Auth([UserRole.MANAGER])
    @PostMapping("/groups/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun addGroupMember(
        @PathVariable groupId: Long,
        @Valid @RequestBody request: com.sight.controllers.http.dto.AddGroupMemberRequest,
    ) {
        if (request.method == "GROUP_MATCHING") {
            groupMatchingService.addMemberToGroup(
                groupId = groupId,
                answerId = request.groupMatchingParams!!.answerId,
            )
        }
    }

    @Auth([UserRole.MANAGER])
    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroup(
        @Valid @RequestBody request: CreateGroupRequest,
    ): CreateGroupResponse {
        if (request.method == "GROUP_MATCHING") {
            val id =
                groupMatchingService.createGroupFromGroupMatching(
                    title = request.title,
                    answerIds = request.groupMatchingParams!!.answerIds,
                    leaderUserId = request.groupMatchingParams.leaderUserId,
                )
            return CreateGroupResponse(id)
        }
        throw IllegalArgumentException("Unsupported method: ${request.method}")
    }
}
