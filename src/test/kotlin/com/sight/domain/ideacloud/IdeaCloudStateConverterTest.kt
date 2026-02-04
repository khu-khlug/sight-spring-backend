package com.sight.domain.ideacloud

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IdeaCloudStateConverterTest {
    private lateinit var converter: IdeaCloudStateConverter

    @BeforeEach
    fun setUp() {
        converter = IdeaCloudStateConverter()
    }

    @Test
    fun `convertToDatabaseColumn은 EXPIRED을 expired으로 변환한다`() {
        // when
        val result = converter.convertToDatabaseColumn(IdeaCloudState.EXPIRED)

        // then
        assertEquals("expired", result)
    }

    @Test
    fun `convertToDatabaseColumn은 PUBLIC을 public으로 변환한다`() {
        // when
        val result = converter.convertToDatabaseColumn(IdeaCloudState.PUBLIC)

        // then
        assertEquals("public", result)
    }

    @Test
    fun `convertToDatabaseColumn은 null을 null로 변환한다`() {
        // when
        val result = converter.convertToDatabaseColumn(null)

        // then
        assertNull(result)
    }

    @Test
    fun `convertToEntityAttribute는 expired을 EXPIRED으로 변환한다`() {
        // when
        val result = converter.convertToEntityAttribute("expired")

        // then
        assertEquals(IdeaCloudState.EXPIRED, result)
    }

    @Test
    fun `convertToEntityAttribute는 public을 PUBLIC으로 변환한다`() {
        // when
        val result = converter.convertToEntityAttribute("public")

        // then
        assertEquals(IdeaCloudState.PUBLIC, result)
    }

    @Test
    fun `convertToEntityAttribute는 null을 null로 변환한다`() {
        // when
        val result = converter.convertToEntityAttribute(null)

        // then
        assertNull(result)
    }

    @Test
    fun `convertToEntityAttribute는 알 수 없는 값을 null로 변환한다`() {
        // when
        val result = converter.convertToEntityAttribute("unknown")

        // then
        assertNull(result)
    }
}
