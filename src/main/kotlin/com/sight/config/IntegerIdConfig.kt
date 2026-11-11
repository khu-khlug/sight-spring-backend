package com.sight.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "integer-id")
data class IntegerIdProperties(
    val nodeId: Int? = null,
) {
    init {
        require(nodeId != null && nodeId in 0..1023) {
            "정수 ID 발급기의 노드 번호를 0부터 1023 사이로 설정해야 합니다."
        }
    }
}

@Configuration
@EnableConfigurationProperties(IntegerIdProperties::class)
class IntegerIdConfig
