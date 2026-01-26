package com.sight.service.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ByteBooleanMapperTest {
    @Test
    fun `값이 0이라면 false로 매핑해야 한다`() {
        val result = ByteBooleanMapper.map(0)
        assertEquals(false, result)
    }

    @Test
    fun `값이 0이 아니라면 true로 매핑해야 한다`() {
        val result = ByteBooleanMapper.map(1)
        assertEquals(true, result)
    }
}
