package com.sight.domain.member

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserStatusConverterTest {
    private val converter = UserStatusConverter()

    @Test
    fun `모든 UserStatus enum 값이 올바른 코드로 변환된다`() {
        assertEquals(-1, converter.convertToDatabaseColumn(UserStatus.INACTIVE))
        assertEquals(0, converter.convertToDatabaseColumn(UserStatus.UNAUTHORIZED))
        assertEquals(1, converter.convertToDatabaseColumn(UserStatus.ACTIVE))
    }

    @Test
    fun `null 값은 null로 변환된다`() {
        assertNull(converter.convertToDatabaseColumn(null))
    }

    @Test
    fun `모든 유효한 코드가 올바른 UserStatus enum으로 변환된다`() {
        assertEquals(UserStatus.INACTIVE, converter.convertToEntityAttribute(-1))
        assertEquals(UserStatus.UNAUTHORIZED, converter.convertToEntityAttribute(0))
        assertEquals(UserStatus.ACTIVE, converter.convertToEntityAttribute(1))
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
        assertEquals("유효하지 않은 UserStatus 코드: 999", exception.message)
    }
}
