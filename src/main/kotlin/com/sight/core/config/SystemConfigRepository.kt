package com.sight.core.config

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SystemConfigRepository : JpaRepository<SystemConfig, String> {
    fun findByConfigKey(configKey: ConfigKey): SystemConfig?
}
