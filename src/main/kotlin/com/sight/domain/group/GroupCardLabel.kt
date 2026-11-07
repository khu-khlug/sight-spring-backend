package com.sight.domain.group

enum class GroupCardLabel(val bit: Int) {
    RED(1 shl 0),
    YELLOW(1 shl 1),
    GREEN(1 shl 2),
    BLUE(1 shl 3),
    PURPLE(1 shl 4),
    LIGHT_GRAY(1 shl 5),
    GRAY(1 shl 6),
    BLACK(1 shl 7),
    ;

    companion object {
        // 카드에 저장하는 레이블 비트마스크의 최대값. 모든 레이블을 지정한 상태다.
        val ALL_BITS: Int = entries.fold(0) { acc, label -> acc or label.bit }

        fun toBits(labels: Set<GroupCardLabel>): Int = labels.fold(0) { acc, label -> acc or label.bit }

        // 허용 범위를 벗어난 비트가 있으면 데이터 오류이므로 예외를 던진다.
        fun fromBits(bits: Int): Set<GroupCardLabel> {
            require(bits and ALL_BITS.inv() == 0) { "허용하지 않는 레이블 값입니다." }
            return entries.filter { bits and it.bit != 0 }.toSet()
        }
    }
}
