package com.sight.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class TipServiceTest {
    private lateinit var tipService: TipService

    @BeforeEach
    fun setUp() {
        tipService = TipService()
    }

    @Test
    fun `getRandomTip은 비어있지 않은 문자열을 반환한다`() {
        // when
        val result = tipService.getRandomTip()

        // then
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `getRandomTip은 TIP 접두사로 시작하는 문자열을 반환한다`() {
        // when
        val result = tipService.getRandomTip()

        // then
        assertTrue(result.startsWith("TIP: "))
    }

    @Test
    fun `getTimeBasedMention은 비어있지 않은 문자열을 반환한다`() {
        // when
        val result = tipService.getTimeBasedMention()

        // then
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `getRandomMention은 비어있지 않은 문자열을 반환한다`() {
        // when
        val result = tipService.getRandomMention()

        // then
        assertTrue(result.isNotBlank())
    }
}
