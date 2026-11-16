package com.sight.domain.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GroupSearchKeywordsTest {
    @Test
    fun `앞뒤 공백·연속 공백·탭·줄바꿈·같은 단어의 반복은 검색어에 영향을 주지 않는다`() {
        val keywords = GroupSearchKeywords.parse("  스프링 \t 백엔드\n스프링  ")

        assertEquals(listOf("스프링", "백엔드"), keywords.terms)
        assertEquals(listOf("Spring"), GroupSearchKeywords.parse("Spring spring SPRING").terms)
    }

    @Test
    fun `NBSP·전각 공백·줄바꿈도 검색어 구분 공백으로 본다`() {
        assertEquals(listOf("가", "나", "다"), GroupSearchKeywords.parse("가 나　다\r\n").terms)
    }

    @Test
    fun `모든 검색어가 순서와 관계없이 들어 있어야 일치한다`() {
        val keywords = GroupSearchKeywords.parse("스프링 백엔드")

        assertTrue(keywords.matches("백엔드 개발에 쓰는 스프링"))
        assertFalse(keywords.matches("스프링만 있는 글"))
    }

    @Test
    fun `대소문자를 구분하지 않고 와일드카드 문자는 글자 그대로 찾는다`() {
        assertTrue(GroupSearchKeywords.parse("kotlin").matches("Learning KOTLIN"))
        assertTrue(GroupSearchKeywords.parse("100%").matches("진행률 100% 달성"))
        assertFalse(GroupSearchKeywords.parse("a_c").matches("abc"))
        assertTrue(GroupSearchKeywords.parse("a\\b").matches("a\\b"))
    }

    @Test
    fun `공백뿐이거나 비어 있는 검색어는 허용하지 않는다`() {
        assertThrows<IllegalArgumentException> { GroupSearchKeywords.parse("") }
        assertThrows<IllegalArgumentException> { GroupSearchKeywords.parse(" \t\n ") }
    }

    @Test
    fun `검색어는 20자까지 허용하고 21자는 허용하지 않는다`() {
        GroupSearchKeywords.parse("가".repeat(20))

        assertThrows<IllegalArgumentException> { GroupSearchKeywords.parse("가".repeat(21)) }
    }

    @Test
    fun `일치하는 검색어 중 가장 앞의 위치를 찾는다`() {
        val keywords = GroupSearchKeywords.parse("백엔드 스프링")

        assertEquals(GroupSearchKeywords.MatchPosition(index = 0, length = 3), keywords.firstMatch("스프링 백엔드"))
        assertNull(keywords.firstMatch("없는 단어"))
    }
}
