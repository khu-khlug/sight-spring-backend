package com.sight.service

import com.sight.core.config.ConfigKey
import com.sight.core.config.SystemConfigRegistry
import com.sight.core.exception.NotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class DoorLockPasswordServiceTest {
    private val systemConfigRegistry: SystemConfigRegistry = mock()
    private lateinit var doorLockPasswordService: DoorLockPasswordService

    @BeforeEach
    fun setUp() {
        doorLockPasswordService = DoorLockPasswordService(systemConfigRegistry)
    }

    @Test
    fun `비밀번호가 모두 설정되어 있으면 조회에 성공한다`() {
        // given
        whenever(systemConfigRegistry.getValue(ConfigKey.DOOR_LOCK_MASTER_PASSWORD)).thenReturn("master123")
        whenever(systemConfigRegistry.getValue(ConfigKey.DOOR_LOCK_JAJUDY_PASSWORD)).thenReturn("jajudy456")
        whenever(systemConfigRegistry.getValue(ConfigKey.DOOR_LOCK_FACILITY_TEAM_PASSWORD)).thenReturn("facility789")

        // when
        val result = doorLockPasswordService.getDoorLockPasswords()

        // then
        assertEquals("master123", result.master)
        assertEquals("jajudy456", result.forJajudy)
        assertEquals("facility789", result.forFacilityTeam)
    }

    @Test
    fun `비밀번호 중 하나라도 설정되지 않으면 NotFoundException이 발생한다`() {
        // given
        whenever(systemConfigRegistry.getValue(ConfigKey.DOOR_LOCK_MASTER_PASSWORD)).thenReturn("master123")
        whenever(systemConfigRegistry.getValue(ConfigKey.DOOR_LOCK_JAJUDY_PASSWORD)).thenReturn("")
        whenever(systemConfigRegistry.getValue(ConfigKey.DOOR_LOCK_FACILITY_TEAM_PASSWORD)).thenReturn("facility789")

        // when & then
        assertThrows<NotFoundException> {
            doorLockPasswordService.getDoorLockPasswords()
        }
    }

    @Test
    fun `비밀번호를 변경하면 SystemConfigRegistry에 3개 키 모두 저장된다`() {
        // when
        doorLockPasswordService.updateDoorLockPasswords(
            master = "newMaster",
            forJajudy = "newJajudy",
            forFacilityTeam = "newFacility",
        )

        // then
        verify(systemConfigRegistry).setValue(ConfigKey.DOOR_LOCK_MASTER_PASSWORD, "newMaster")
        verify(systemConfigRegistry).setValue(ConfigKey.DOOR_LOCK_JAJUDY_PASSWORD, "newJajudy")
        verify(systemConfigRegistry).setValue(ConfigKey.DOOR_LOCK_FACILITY_TEAM_PASSWORD, "newFacility")
    }
}
