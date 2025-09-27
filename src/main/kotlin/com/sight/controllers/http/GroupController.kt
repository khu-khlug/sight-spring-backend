package com.sight.controllers.http

import com.sight.controllers.http.dto.CreateGroupDiscordChannelResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.service.GroupDiscordChannelService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
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
}
