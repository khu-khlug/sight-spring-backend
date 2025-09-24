package com.sight.domain.member

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StudentStatusConverterTest {
    private val converter = StudentStatusConverter()

    @Test
    fun `모든 StudentStatus enum 값이 올바른 코드로 변환된다`() {
        assertEquals(-1, converter.convertToDatabaseColumn(StudentStatus.UNITED))
        assertEquals(0, converter.convertToDatabaseColumn(StudentStatus.ABSENCE))
        assertEquals(1, converter.convertToDatabaseColumn(StudentStatus.UNDERGRADUATE))
        assertEquals(2, converter.convertToDatabaseColumn(StudentStatus.GRADUATE))
    }

    @Test
    fun `null 값은 null로 변환된다`() {
        assertNull(converter.convertToDatabaseColumn(null))
    }

    @Test
    fun `모든 유효한 코드가 올바른 StudentStatus enum으로 변환된다`() {
        assertEquals(StudentStatus.UNITED, converter.convertToEntityAttribute(-1))
        assertEquals(StudentStatus.ABSENCE, converter.convertToEntityAttribute(0))
        assertEquals(StudentStatus.UNDERGRADUATE, converter.convertToEntityAttribute(1))
        assertEquals(StudentStatus.GRADUATE, converter.convertToEntityAttribute(2))
    }

    @Test
    fun `null 코드는 null로 변환된다`() {
        assertNull(converter.convertToEntityAttribute(null))
    }

    @Test
    fun `존재하지 않는 코드는 예외를 발생시킨다`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                converter.convertToEntityAttribute(999)
            }
        assertEquals("유효하지 않은 StudentStatus 코드: 999", exception.message)
    }
}
