package com.sight.domain.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class GroupRecordTest {
    @Test
    fun `같은 본문으로 수정해도 수정 시각을 갱신한다`() {
        val record = record()

        record.edit("before", LATER)

        assertEquals("before", record.content)
        assertEquals(LATER, record.updatedAt)
    }

    @Test
    fun `수정 요청의 본문을 저장하고 수정 시각을 갱신한다`() {
        val record = record()

        record.edit("after", LATER)

        assertEquals("after", record.content)
        assertEquals(LATER, record.updatedAt)
    }

    @Test
    fun `수정 가능 여부를 검사할 때는 본문과 수정 시각을 변경하지 않는다`() {
        val record = record()

        record.validateEdit("after")

        assertEquals("before", record.content)
        assertEquals(NOW, record.updatedAt)
    }

    @Test
    fun `기록 본문을 빈 문자열로 수정할 수 없다`() {
        val record = record()

        assertThrows<IllegalArgumentException> { record.edit("", LATER) }

        assertEquals("before", record.content)
        assertEquals(NOW, record.updatedAt)
    }

    @Test
    fun `삭제된 기록은 수정할 수 없다`() {
        val record = record().apply { delete(NOW) }

        assertThrows<IllegalArgumentException> { record.edit("before", LATER) }

        assertEquals(NOW, record.updatedAt)
    }

    @Test
    fun `본문 크기가 UTF-8 기준 1 MiB를 넘으면 수정할 수 없다`() {
        val record = record()
        val content = "\uD83D\uDE00".repeat(GroupRecord.CONTENT_MAX_BYTES / 4)

        record.edit(content, LATER)
        assertThrows<IllegalArgumentException> { record.edit(content + "x", LATER.plusSeconds(1)) }

        assertEquals(content, record.content)
        assertEquals(LATER, record.updatedAt)
    }

    private fun record() = GroupRecord(1L, 2L, 3L, "before", NOW)

    companion object {
        private val NOW = Instant.parse("2026-11-01T00:00:00Z")
        private val LATER = NOW.plusSeconds(1)
    }
}
