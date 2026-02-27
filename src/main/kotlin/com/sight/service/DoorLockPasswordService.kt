package com.sight.service

import com.sight.core.config.ConfigKey
import com.sight.core.config.SystemConfigRegistry
import com.sight.core.exception.NotFoundException
import org.springframework.stereotype.Service

data class DoorLockPasswordInfo(
    val master: String,
    val forJajudy: String,
    val forFacilityTeam: String,
)

@Service
class DoorLockPasswordService(
    private val systemConfigRegistry: SystemConfigRegistry,
) {
    fun getDoorLockPasswords(): DoorLockPasswordInfo {
        val master = systemConfigRegistry.getValue(ConfigKey.DOOR_LOCK_MASTER_PASSWORD)
        val forJajudy = systemConfigRegistry.getValue(ConfigKey.DOOR_LOCK_JAJUDY_PASSWORD)
        val forFacilityTeam = systemConfigRegistry.getValue(ConfigKey.DOOR_LOCK_FACILITY_TEAM_PASSWORD)

        if (master.isBlank() || forJajudy.isBlank() || forFacilityTeam.isBlank()) {
            throw NotFoundException("일부 도어락 비밀번호가 존재하지 않습니다")
        }

        return DoorLockPasswordInfo(
            master = master,
            forJajudy = forJajudy,
            forFacilityTeam = forFacilityTeam,
        )
    }

    fun updateDoorLockPasswords(
        master: String,
        forJajudy: String,
        forFacilityTeam: String,
    ) {
        systemConfigRegistry.setValue(ConfigKey.DOOR_LOCK_MASTER_PASSWORD, master)
        systemConfigRegistry.setValue(ConfigKey.DOOR_LOCK_JAJUDY_PASSWORD, forJajudy)
        systemConfigRegistry.setValue(ConfigKey.DOOR_LOCK_FACILITY_TEAM_PASSWORD, forFacilityTeam)
    }
}
