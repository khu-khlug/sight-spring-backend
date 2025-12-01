package com.sight.controllers.http

import com.sight.controllers.http.dto.AddGroupDiscordChannelMemberRequest
import com.sight.controllers.http.dto.CreateGroupDiscordChannelResponse
import com.sight.controllers.http.dto.CreateGroupRequest
import com.sight.controllers.http.dto.CreateGroupResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupDiscordChannelService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupController(
    private val groupDiscordChannelService: GroupDiscordChannelService,
    private val groupMatchingService: com.sight.service.GroupMatchingService,
) {
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
