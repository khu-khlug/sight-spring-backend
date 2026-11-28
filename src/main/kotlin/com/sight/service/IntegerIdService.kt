package com.sight.service

import com.sight.config.IntegerIdProperties
import com.sight.core.id.TsidGenerator
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service

// 각 JVM에 지정한 노드 번호로 생성기 하나를 만들고 모든 호출에서 공유한다.
@Service
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
        check(id > LEGACY_MAX_ID) { "생성된 ID가 기존 식별자 범위와 겹칩니다." }
        return id
    }

    companion object {
        // 기존 정수 ID의 상한. 2027-02 말 기준으로 기존 채번식이 이론상 낼 수 있는 최댓값은
        // 분 단위 식 1,000,000 + 1,136,160분 × 1000 + 999 = 약 11.4억이다.
        // 이보다 크고 기본 epoch의 TSID(약 9 × 10^17)보다 충분히 작은 20억으로 둔다.
        private const val LEGACY_MAX_ID = 2_000_000_000L
    }
}
