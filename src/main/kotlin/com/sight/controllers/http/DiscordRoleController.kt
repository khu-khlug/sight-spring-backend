package com.sight.controllers.http

import com.sight.controllers.http.dto.GetDiscordRoleResponse
import com.sight.controllers.http.dto.UpdateDiscordRoleRequest
import com.sight.core.auth.Auth
import com.sight.core.auth.UserRole
import com.sight.service.DiscordRoleService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DiscordRoleController(
    private val discordRoleService: DiscordRoleService,
) {
    @Auth([UserRole.MANAGER])
    @GetMapping("/manager/discord-roles")
    fun getDiscordRoles(): List<GetDiscordRoleResponse> {
        val discordRoles = discordRoleService.getAllDiscordRoles()
        return discordRoles.map { discordRole ->
            GetDiscordRoleResponse(
                id = discordRole.id!!,
                roleType = discordRole.roleType,
                roleId = discordRole.roleId,
                createdAt = discordRole.createdAt,
                updatedAt = discordRole.updatedAt,
            )
        }
    }

    @Auth([UserRole.MANAGER])
    @PutMapping("/manager/discord-roles/{id}")
    fun updateDiscordRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateDiscordRoleRequest,
    ): GetDiscordRoleResponse {
        val updatedRole = discordRoleService.updateDiscordRole(id, request.roleId)
        return GetDiscordRoleResponse(
            id = updatedRole.id!!,
            roleType = updatedRole.roleType,
            roleId = updatedRole.roleId,
            createdAt = updatedRole.createdAt,
            updatedAt = updatedRole.updatedAt,
        )
    }
}
