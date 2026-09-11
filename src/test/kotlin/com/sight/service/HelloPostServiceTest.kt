package com.sight.service

import com.sight.repository.HelloPostRepository
import com.sight.repository.dto.HelloPostView
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HelloPostServiceTest {
    private val helloPostRepository: HelloPostRepository = mock()
    private lateinit var helloPostService: HelloPostService

    @BeforeEach
    fun setUp() {
        helloPostService = HelloPostService(helloPostRepository)
    }

    @Test
    fun `listHelloPosts는 최신 Hello 게시물 목록을 반환한다`() {
        val date = Instant.parse("2026-09-01T09:00:00Z")
        given(helloPostRepository.findRecentPosts(10))
            .willReturn(
                listOf(
                    HelloPostView(
                        id = 1L,
                        category = null,
                        title = "2026년 2학기 OT를 진행했습니다",
                        date = date,
                    ),
                ),
            )

        val result = helloPostService.listHelloPosts(10)

        assertEquals(1, result.size)
        assertEquals(1L, result.single().id)
        assertNull(result.single().category)
        assertEquals("2026년 2학기 OT를 진행했습니다", result.single().title)
        assertEquals(date, result.single().date)
        verify(helloPostRepository).findRecentPosts(10)
    }

    @Test
    fun `listHelloPosts는 게시물이 없으면 빈 목록을 반환한다`() {
        given(helloPostRepository.findRecentPosts(3)).willReturn(emptyList())

        val result = helloPostService.listHelloPosts(3)

        assertEquals(emptyList(), result)
        verify(helloPostRepository).findRecentPosts(3)
    }
}
