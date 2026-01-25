package com.sight.service

import com.sight.repository.DocumentRepository
import com.sight.repository.projection.TalkWithAuthorProjection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals

class TalkServiceTest {
    private val documentRepository: DocumentRepository = mock()
    private lateinit var talkService: TalkService

    @BeforeEach
    fun setUp() {
        talkService = TalkService(documentRepository)
    }

    @Test
    fun `listTalks는 담소 목록과 총 개수를 반환한다`() {
        // given
        val offset = 0
        val limit = 10
        val now = LocalDateTime.now()

        val talkProjection =
            object : TalkWithAuthorProjection {
                override val id: Long = 1L
                override val title: String = "테스트 담소"
                override val authorId: Long = 100L
                override val authorRealname: String = "테스트 사용자"
                override val createdAt: LocalDateTime = now
            }

        whenever(
            documentRepository.findTalksWithAuthor(
                TalkService.TALKS_BOARD_ID,
                TalkService.STATE_PUBLIC,
                offset,
                limit,
            ),
        ).thenReturn(listOf(talkProjection))

        whenever(
            documentRepository.countByBoardAndState(
                TalkService.TALKS_BOARD_ID,
                TalkService.STATE_PUBLIC,
            ),
        ).thenReturn(1L)

        // when
        val result = talkService.listTalks(offset, limit)

        // then
        assertEquals(1L, result.count)
        assertEquals(1, result.talks.size)

        val talk = result.talks[0]
        assertEquals(1L, talk.id)
        assertEquals("테스트 담소", talk.title)
        assertEquals(100L, talk.authorId)
        assertEquals("테스트 사용자", talk.authorRealname)
    }

    @Test
    fun `listTalks는 담소가 없을 때 빈 목록을 반환한다`() {
        // given
        val offset = 0
        val limit = 10

        whenever(
            documentRepository.findTalksWithAuthor(
                TalkService.TALKS_BOARD_ID,
                TalkService.STATE_PUBLIC,
                offset,
                limit,
            ),
        ).thenReturn(emptyList())

        whenever(
            documentRepository.countByBoardAndState(
                TalkService.TALKS_BOARD_ID,
                TalkService.STATE_PUBLIC,
            ),
        ).thenReturn(0L)

        // when
        val result = talkService.listTalks(offset, limit)

        // then
        assertEquals(0L, result.count)
        assertEquals(0, result.talks.size)
    }
}
