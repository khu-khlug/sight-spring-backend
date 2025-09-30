package com.sight.controllers.http

import com.sight.controllers.http.dto.AddGroupDiscordChannelMemberRequest
import com.sight.controllers.http.dto.CheckUserInGroupDiscordChannelResponse
import com.sight.controllers.http.dto.CreateGroupDiscordChannelResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupDiscordChannelService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupController(
    private val groupDiscordChannelService: GroupDiscordChannelService,
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

    @Auth([UserRole.USER, UserRole.MANAGER])
    @GetMapping("/groups/{groupId}/members/@me/discord-participation")
    fun checkUserInGroupDiscordChannel(
        @PathVariable groupId: Long,
        requester: Requester,
    ): CheckUserInGroupDiscordChannelResponse {
        val isInChannel =
            groupDiscordChannelService.checkUserInDiscordChannel(
                groupId = groupId,
                userId = requester.userId,
            )

        return CheckUserInGroupDiscordChannelResponse(
            isInChannel = isInChannel,
        )
    }
}
