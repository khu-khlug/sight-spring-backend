package com.sight.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "integer-id")
data class IntegerIdProperties(
    val enabled: Boolean = false,
    val nodeId: Int? = null,
    val legacyMaxId: Long? = null,
) {
    init {
        require(!enabled || (legacyMaxId != null && legacyMaxId >= 0 && legacyMaxId < Long.MAX_VALUE)) {
            "정수 ID 발급을 활성화하려면 확인한 기존 ID 최댓값을 설정해야 합니다."
        }
        require((!enabled && nodeId == null) || (nodeId != null && nodeId in 0..1023)) {
            "정수 ID 발급기의 노드 번호를 0부터 1023 사이로 설정해야 합니다."
        }
    }
}

@Configuration
@EnableConfigurationProperties(IntegerIdProperties::class)
class IntegerIdConfig
