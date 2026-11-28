package com.sight.domain.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.text.Normalizer

class GroupSearchKeywordsTest {
    private fun nfd(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD)

    // 운영 코드는 일치 위치까지 필요해 `find`만 쓰므로, 일치 여부만 보는 테스트에서만 쓰는 도우미다.
    private fun GroupSearchKeywords.matches(text: String): Boolean = find(text) != null

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
    fun `대소문자를 구분하지 않고 와일드카드 문자와 역슬래시는 글자 그대로 찾는다`() {
        assertTrue(GroupSearchKeywords.parse("kotlin").matches("Learning KOTLIN"))
        assertTrue(GroupSearchKeywords.parse("100%").matches("진행률 100% 달성"))
        assertFalse(GroupSearchKeywords.parse("100%").matches("진행률 1000 달성"))
        assertFalse(GroupSearchKeywords.parse("a_c").matches("abc"))
        assertTrue(GroupSearchKeywords.parse("a\\b").matches("a\\b"))
        assertTrue(GroupSearchKeywords.parse("!").matches("느낌표! 포함"))
    }

    @Test
    fun `악센트와 전각·반각은 서로 다른 글자로 구분한다`() {
        assertFalse(GroupSearchKeywords.parse("cafe").matches("café"))
        assertFalse(GroupSearchKeywords.parse("café").matches("cafe"))
        assertTrue(GroupSearchKeywords.parse("CAFÉ").matches("café"))
        assertFalse(GroupSearchKeywords.parse("cafe").matches("ＣＡＦＥ"))
        assertFalse(GroupSearchKeywords.parse("ＣＡＦＥ").matches("cafe"))
        // 같은 전각 문자끼리는 대소문자만 무시한다.
        assertTrue(GroupSearchKeywords.parse("ＣＡＦＥ").matches("ｃａｆｅ 메뉴"))
    }

    @Test
    fun `풀어쓴 형태와 합친 형태는 NFC로 정규화해 같은 글자로 본다`() {
        // 맥 파일명 등에서 올 수 있는 풀어쓴 한글(자모)과 악센트 분리 문자.
        assertTrue(GroupSearchKeywords.parse("한글").matches(nfd("한글 검색")))
        assertTrue(GroupSearchKeywords.parse(nfd("한글")).matches("한글 검색"))
        assertTrue(GroupSearchKeywords.parse("café").matches(nfd("café")))
        assertTrue(GroupSearchKeywords.parse(nfd("café")).matches("café"))
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
    fun `이모지처럼 UTF-16 두 단위인 글자도 한 글자로 센다`() {
        GroupSearchKeywords.parse("😀".repeat(20))

        assertThrows<IllegalArgumentException> { GroupSearchKeywords.parse("😀".repeat(21)) }
    }

    @Test
    fun `글자 수는 NFC로 정규화한 뒤의 글자 수로 센다`() {
        // 풀어쓴 한글은 코드 포인트가 40개지만 정규화하면 20자다.
        GroupSearchKeywords.parse(nfd("가".repeat(20)))

        assertThrows<IllegalArgumentException> { GroupSearchKeywords.parse(nfd("가".repeat(21))) }
    }

    @Test
    fun `일치하면 정규화한 텍스트와 가장 앞에 나오는 검색어의 위치를 반환한다`() {
        val keywords = GroupSearchKeywords.parse("백엔드 스프링")

        val match = keywords.find("  스프링   백엔드 ")!!

        assertEquals("스프링 백엔드", match.text)
        assertEquals(0, match.index)
        assertEquals(3, match.length)
        assertNull(keywords.find("스프링만 있음"))
    }
}
