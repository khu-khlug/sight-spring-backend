package com.sight.service

import com.sight.domain.ideacloud.IdeaCloud
import com.sight.domain.ideacloud.IdeaCloudState
import com.sight.repository.IdeaCloudRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TipServiceTest {
    private lateinit var ideaCloudRepository: IdeaCloudRepository
    private lateinit var tipService: TipService

    @BeforeEach
    fun setUp() {
        ideaCloudRepository = mock()
        tipService = TipService(ideaCloudRepository)
    }

    @Test
    fun `getRandomTip은 비어있지 않은 문자열을 반환한다`() {
        // when
        val result = tipService.getRandomTip()

        // then
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `getRandomTip은 TIP 접두사로 시작하는 문자열을 반환한다`() {
        // when
        val result = tipService.getRandomTip()

        // then
        assertTrue(result.startsWith("TIP: "))
    }

    @Test
    fun `getTimeBasedMention은 비어있지 않은 문자열을 반환한다`() {
        // when
        val result = tipService.getTimeBasedMention()

        // then
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `getRandomMention은 비어있지 않은 문자열을 반환한다`() {
        // when
        val result = tipService.getRandomMention()

        // then
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `getRandomIdea는 공개된 아이디어가 있으면 포맷된 문자열을 반환한다`() {
        // given
        val idea =
            IdeaCloud(
                id = 1L,
                idea = "테스트 아이디어",
                author = 1L,
                state = IdeaCloudState.PUBLIC,
            )
        whenever(ideaCloudRepository.findRandomPublicIdea()).thenReturn(idea)

        // when
        val result = tipService.getRandomIdea()

        // then
        assertEquals("이걸 만들어보는 건 어때요?: 테스트 아이디어", result)
    }

    @Test
    fun `getRandomIdea는 공개된 아이디어가 없으면 기본 메시지를 반환한다`() {
        // given
        whenever(ideaCloudRepository.findRandomPublicIdea()).thenReturn(null)

        // when
        val result = tipService.getRandomIdea()

        // then
        assertEquals("좋은 아이디어가 떠오르질 않네요...", result)
    }
}
