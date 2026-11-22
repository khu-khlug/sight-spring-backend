package com.sight.domain.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class GroupRecordTest {
    @Test
    fun `첨부만 바뀌어도 수정 메서드 한 번으로 수정 시각을 갱신한다`() {
        val record = record()

        assertTrue(record.edit("before", true, LATER))

        assertEquals("before", record.content)
        assertEquals(LATER, record.updatedAt)
    }

    @Test
    fun `본문이 바뀌면 첨부 변경 없이도 수정 시각을 갱신한다`() {
        val record = record()

        assertTrue(record.edit("after", false, LATER))

        assertEquals("after", record.content)
        assertEquals(LATER, record.updatedAt)
    }

    @Test
    fun `본문과 첨부가 모두 같으면 수정 시각을 유지한다`() {
        val record = record()

        assertFalse(record.edit("before", false, LATER))

        assertEquals(NOW, record.updatedAt)
    }

    @Test
    fun `수정 전 검증은 기록의 본문과 수정 시각을 바꾸지 않는다`() {
        val record = record()

        record.validateEdit("after")

        assertEquals("before", record.content)
        assertEquals(NOW, record.updatedAt)
    }

    @Test
    fun `수정은 유효하지 않은 본문을 첨부 변경 여부와 관계없이 거부한다`() {
        val record = record()

        assertThrows<IllegalArgumentException> { record.edit("", true, LATER) }

        assertEquals("before", record.content)
        assertEquals(NOW, record.updatedAt)
    }

    @Test
    fun `삭제된 기록은 첨부만 바뀌어도 수정할 수 없다`() {
        val record = record().apply { delete(NOW) }

        assertThrows<IllegalArgumentException> { record.edit("before", true, LATER) }

        assertEquals(NOW, record.updatedAt)
    }

    @Test
    fun `본문 제한은 문자 수가 아니라 UTF8 바이트 수로 적용한다`() {
        val record = record()
        val content = "\uD83D\uDE00".repeat(GroupRecord.CONTENT_MAX_BYTES / 4)

        assertTrue(record.edit(content, false, LATER))
        assertThrows<IllegalArgumentException> { record.edit(content + "x", true, LATER.plusSeconds(1)) }

        assertEquals(content, record.content)
        assertEquals(LATER, record.updatedAt)
    }

    private fun record() = GroupRecord(1L, 2L, 3L, "before", NOW)

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
        private val LATER = NOW.plusSeconds(1)
    }
}
