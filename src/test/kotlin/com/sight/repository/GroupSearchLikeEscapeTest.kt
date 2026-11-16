package com.sight.repository

import com.querydsl.jpa.HQLTemplates
import com.querydsl.jpa.JPQLSerializer
import com.querydsl.jpa.impl.JPAQuery
import com.sight.domain.group.QGroupCard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// 검색어를 글자 그대로 찾는지 확인한다. 데이터베이스 없이 Querydsl이 만드는 JPQL과 바인딩 값을 검증한다.
class GroupSearchLikeEscapeTest {
    private val card = QGroupCard.groupCard

    private fun serialize(keyword: String): JPQLSerializer {
        val query = JPAQuery<Any>(null, HQLTemplates.DEFAULT).from(card).where(card.title.containsIgnoreCase(keyword))
        return JPQLSerializer(HQLTemplates.DEFAULT).also { it.serialize(query.metadata, false, null) }
    }

    @Test
    fun `와일드카드 문자와 이스케이프 문자는 이스케이프해 파라미터로 바인딩한다`() {
        val serializer = serialize("a%b_c!d")

        assertTrue(serializer.toString().contains("escape '!'"))
        assertEquals(listOf("%a!%b!_c!!d%"), serializer.constants)
    }

    @Test
    fun `역슬래시는 이스케이프 문자가 아니므로 글자 그대로 찾는다`() {
        assertEquals(listOf("%a\\b%"), serialize("a\\b").constants)
    }

    @Test
    fun `대소문자를 구분하지 않고 검색어는 쿼리 문자열에 직접 넣지 않는다`() {
        val serializer = serialize("SpRiNg")

        assertEquals(listOf("%spring%"), serializer.constants)
        assertFalse(serializer.toString().contains("SpRiNg", ignoreCase = true))
    }
}
