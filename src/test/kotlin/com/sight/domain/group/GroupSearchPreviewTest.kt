package com.sight.domain.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupSearchPreviewTest {
    @Test
    fun `짧은 본문은 그대로 반환한다`() {
        val preview = GroupSearchPreview.create("스프링 백엔드 스터디", GroupSearchKeywords.parse("백엔드"))

        assertEquals("스프링 백엔드 스터디", preview)
    }

    @Test
    fun `긴 본문은 일치 위치 주변만 잘라 생략 표시를 붙인다`() {
        val text = "가".repeat(100) + "스프링" + "나".repeat(200)

        val preview = GroupSearchPreview.create(text, GroupSearchKeywords.parse("스프링"))

        assertTrue(preview.startsWith("…"))
        assertTrue(preview.endsWith("…"))
        assertTrue(preview.contains("스프링"))
        assertEquals(1 + GroupSearchPreview.BEFORE_LENGTH + 3 + GroupSearchPreview.AFTER_LENGTH + 1, preview.length)
    }

    @Test
    fun `이모지를 중간에서 자르지 않는다`() {
        val text = "😀".repeat(60) + "스프링" + "😀".repeat(60)

        val preview = GroupSearchPreview.create(text, GroupSearchKeywords.parse("스프링"))

        // 짝이 맞지 않는 서로게이트 문자가 없어야 한다.
        assertTrue(preview.codePoints().allMatch { it !in 0xD800..0xDFFF })
    }
}
