package com.sight.core.id

import com.github.f4b6a3.tsid.TsidFactory
import java.time.Instant

// JVM마다 다른 노드 번호를 지정하고, 해당 JVM에서는 이 생성기 하나를 공유한다.
class TsidGenerator(
    nodeId: Int,
) {
    init {
        require(nodeId in 0 until NODE_COUNT) { "TSID 노드 번호는 0부터 1023까지여야 합니다." }
    }

    private val factory: TsidFactory =
        TsidFactory.builder()
            .withNodeBits(NODE_BITS)
            .withNode(nodeId)
            .withCustomEpoch(Instant.ofEpochMilli(EPOCH_MILLIS))
            .withTimeFunction {
                System.currentTimeMillis().also { timestamp ->
                    check(timestamp in EPOCH_MILLIS..MAX_TIMESTAMP_MILLIS) {
                        "현재 시각이 TSID 식별자를 발급할 수 있는 범위를 벗어났습니다."
                    }
                }
            }
            .build()

    fun nextId(): Long {
        val id = factory.create().toLong()
        // 카운터 초과나 시계 역행으로 논리 시각이 앞서더라도 양의 BIGINT 수명을 넘길 수 없다.
        check(id > 0) { "TSID 식별자가 양의 Long 범위를 벗어났습니다." }
        return id
    }

    companion object {
        // TSID Creator의 기본 기준 시각인 2020-01-01T00:00:00Z의 Unix 밀리초 값.
        // 발급 시 현재 시각에서 이 값을 빼 시간 부분을 만들고, 해석 시 다시 더해 실제 시각을 복원한다.
        // 모든 발급 서버가 같은 값을 사용해야 하며 기존 ID와의 호환성 없이 변경하면 안 된다.
        const val EPOCH_MILLIS: Long = 1_577_836_800_000L
        const val NODE_BITS: Int = 10
        const val NODE_COUNT: Int = 1024
        const val RANDOM_BITS: Int = 22
        const val MAX_LOGICAL_TIMESTAMP_MILLIS: Long = 2_199_023_255_551L
        const val MAX_TIMESTAMP_MILLIS: Long = EPOCH_MILLIS + MAX_LOGICAL_TIMESTAMP_MILLIS

        // TSID에 담긴 논리 시각을 Unix 밀리초로 복원한다.
        fun timestampMillis(id: Long): Long {
            require(id > 0) { "TSID 식별자는 양의 Long 정수여야 합니다." }
            return EPOCH_MILLIS + (id ushr RANDOM_BITS)
        }
    }
}
