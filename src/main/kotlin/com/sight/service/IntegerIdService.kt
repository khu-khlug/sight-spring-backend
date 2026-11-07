package com.sight.service

import com.sight.config.IntegerIdProperties
import com.sight.core.id.TsidGenerator
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

// 각 JVM에 지정한 노드 번호로 생성기 하나를 만들고 모든 호출에서 공유한다.
@Service
@ConditionalOnProperty(prefix = "integer-id", name = ["enabled"], havingValue = "true")
class IntegerIdService(
    private val properties: IntegerIdProperties,
) {
    private val generator = TsidGenerator(requireNotNull(properties.nodeId))

    @PostConstruct
    fun initialize() {
        // 시작 시 설정·시계·기존 ID 경계를 확인한다. 이 첫 값은 외부에 반환하지 않는다.
        nextId()
    }

    fun nextId(): Long {
        val id = generator.nextId()
        check(id > requireNotNull(properties.legacyMaxId)) { "생성된 ID가 기존 식별자 범위와 겹칩니다." }
        return id
    }
}
