package com.sight.domain.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupSearchPreviewTest {
    private fun preview(
        text: String,
        keyword: String,
    ): String = GroupSearchPreview.create(GroupSearchKeywords.parse(keyword).find(text)!!)

    private fun String.codePointLength(): Int = codePointCount(0, length)

    @Test
    fun `짧은 본문은 그대로 반환한다`() {
        assertEquals("스프링 백엔드 스터디", preview("스프링 백엔드 스터디", "백엔드"))
    }

    @Test
    fun `긴 본문은 일치 위치 주변만 잘라 생략 표시를 붙인다`() {
        val text = "가".repeat(100) + "스프링" + "나".repeat(200)

        val preview = preview(text, "스프링")

        assertTrue(preview.startsWith("…"))
        assertTrue(preview.endsWith("…"))
        assertTrue(preview.contains("스프링"))
        assertEquals(1 + GroupSearchPreview.BEFORE_LENGTH + 3 + GroupSearchPreview.AFTER_LENGTH + 1, preview.codePointLength())
    }

    @Test
    fun `본문 앞에서 일치하면 앞쪽 생략 표시가 없다`() {
        val preview = preview("스프링" + "나".repeat(200), "스프링")

        assertFalse(preview.startsWith("…"))
        assertTrue(preview.endsWith("…"))
    }

    @Test
    fun `이모지를 중간에서 자르지 않고 글자 수 단위로 자른다`() {
        val text = "😀".repeat(60) + "스프링" + "😀".repeat(120)

        val preview = preview(text, "스프링")

        // 짝이 맞지 않는 서로게이트 문자가 없어야 한다.
        assertTrue(preview.codePoints().allMatch { it !in 0xD800..0xDFFF })
        assertEquals(1 + GroupSearchPreview.BEFORE_LENGTH + 3 + GroupSearchPreview.AFTER_LENGTH + 1, preview.codePointLength())
    }

    @Test
    fun `일치한 검색어가 이모지 사이에 있어도 이모지를 보호한다`() {
        val text = "a" + "😀".repeat(80) + "스프링"

        val preview = preview(text, "스프링")

        assertTrue(preview.codePoints().allMatch { it !in 0xD800..0xDFFF })
    }
}
