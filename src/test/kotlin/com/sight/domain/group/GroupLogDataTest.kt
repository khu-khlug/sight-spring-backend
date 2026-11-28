package com.sight.domain.group

import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals

class GroupLogDataTest {
    @Test
    fun `LEGACY를 제외한 모든 로그타입에 로그데이터가 하나씩 있다`() {
        val logTypes = allLogData().map { it.logType }

        assertEquals(logTypes.size, logTypes.toSet().size)
        assertEquals(GroupLogType.entries.toSet() - GroupLogType.LEGACY, logTypes.toSet())
    }

    // 생성자 인자 타입별 임의 값으로 모든 로그데이터를 하나씩 만든다.
    private fun allLogData(): List<GroupLogData> =
        GroupLogData::class.sealedSubclasses.map { subclass ->
            subclass.objectInstance ?: create(subclass)
        }

    private fun create(subclass: KClass<out GroupLogData>): GroupLogData {
        val constructor = requireNotNull(subclass.primaryConstructor)
        return constructor.callBy(
            constructor.parameters.associateWith { parameter ->
                when (parameter.type.classifier) {
                    Long::class -> 1L
                    Int::class -> 2026
                    String::class -> "값"
                    List::class -> listOf("항목")
                    GroupCategory::class -> GroupCategory.STUDY
                    else -> error("${subclass.simpleName}.${parameter.name}의 테스트 값이 없습니다.")
                }
            },
        )
    }
}
