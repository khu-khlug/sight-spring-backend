package com.sight.service

import com.sight.domain.discord.DiscordRole
import com.sight.domain.discord.DiscordRoleType
import com.sight.repository.DiscordRoleRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

class DiscordRoleServiceTest {
    private val discordRoleRepository: DiscordRoleRepository = mock()
    private lateinit var discordRoleService: DiscordRoleService

    @BeforeEach
    fun setUp() {
        discordRoleService = DiscordRoleService(discordRoleRepository)
    }

    @Test
    fun `getAllDiscordRoles는 모든 디스코드 역할을 반환한다`() {
        val mockRoles =
            listOf(
                DiscordRole(
                    id = 1L,
                    roleType = DiscordRoleType.MEMBER,
                    roleId = "123456789",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
                DiscordRole(
                    id = 2L,
                    roleType = DiscordRoleType.MANAGER,
                    roleId = "987654321",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                ),
            )

        whenever(discordRoleRepository.findAll()).thenReturn(mockRoles)

        val result = discordRoleService.getAllDiscordRoles()

        assertEquals(2, result.size)
        assertEquals(DiscordRoleType.MEMBER, result[0].roleType)
        assertEquals(DiscordRoleType.MANAGER, result[1].roleType)
        verify(discordRoleRepository).findAll()
    }

    @Test
    fun `updateDiscordRole은 존재하는 역할의 roleId를 업데이트한다`() {
        val roleId = 1L
        val newRoleId = "new-role-id"
        val existingRole =
            DiscordRole(
                id = roleId,
                roleType = DiscordRoleType.MEMBER,
                roleId = "old-role-id",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now().minusHours(1),
            )
        val updatedRole =
            existingRole.copy(
                roleId = newRoleId,
                updatedAt = LocalDateTime.now(),
            )

        whenever(discordRoleRepository.findById(roleId)).thenReturn(Optional.of(existingRole))
        whenever(discordRoleRepository.save(any<DiscordRole>())).thenReturn(updatedRole)

        val result = discordRoleService.updateDiscordRole(roleId, newRoleId)

        assertEquals(newRoleId, result.roleId)
        assertNotEquals(existingRole.updatedAt, result.updatedAt)
        verify(discordRoleRepository).findById(roleId)
        verify(discordRoleRepository).save(any<DiscordRole>())
    }

    @Test
    fun `updateDiscordRole은 존재하지 않는 역할에 대해 예외를 발생시킨다`() {
        val roleId = 999L
        val newRoleId = "new-role-id"

        whenever(discordRoleRepository.findById(roleId)).thenReturn(Optional.empty())

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                discordRoleService.updateDiscordRole(roleId, newRoleId)
            }

        assertEquals("Discord role not found with id: $roleId", exception.message)
        verify(discordRoleRepository).findById(roleId)
    }
}
