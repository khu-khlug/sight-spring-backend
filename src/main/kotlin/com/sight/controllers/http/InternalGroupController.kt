package com.sight.controllers.http

import com.sight.controllers.http.dto.GetGroupDiscordChannelResponse
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.GroupDiscordChannelService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalGroupController(
    private val groupDiscordChannelService: GroupDiscordChannelService,
) {
    @Auth(roles = [UserRole.SYSTEM])
    @GetMapping("/internal/groups/{groupId}/discord-channel")
    fun getGroupDiscordChannel(
        @PathVariable groupId: Long,
    ): GetGroupDiscordChannelResponse {
        val groupDiscordChannel = groupDiscordChannelService.getDiscordChannelByGroupId(groupId)

        return GetGroupDiscordChannelResponse(
            id = groupDiscordChannel.id,
            groupId = groupDiscordChannel.groupId,
            discordChannelId = groupDiscordChannel.discordChannelId,
            createdAt = groupDiscordChannel.createdAt,
            updatedAt = groupDiscordChannel.updatedAt,
        )
    }
}
