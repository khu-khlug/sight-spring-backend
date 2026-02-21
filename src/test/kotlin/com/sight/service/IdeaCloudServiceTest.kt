package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.domain.ideacloud.IdeaCloud
import com.sight.domain.ideacloud.IdeaCloudState
import com.sight.repository.IdeaCloudRepository
import com.sight.repository.projection.IdeaCloudWithAuthorProjection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals

class IdeaCloudServiceTest {
    private val ideaCloudRepository: IdeaCloudRepository = mock()
    private val pointService: PointService = mock()
    private lateinit var ideaCloudService: IdeaCloudService

    @BeforeEach
    fun setUp() {
        ideaCloudService = IdeaCloudService(ideaCloudRepository, pointService)
    }

    @Test
    fun `listRandomPublicIdeas는 공개 아이디어 목록을 반환한다`() {
        // given
        val projection1 = createMockProjection(1L, "아이디어1", 100L, "작성자1")
        val projection2 = createMockProjection(2L, "아이디어2", 101L, "작성자2")
        given(ideaCloudRepository.findRandomPublicIdeasWithAuthor())
            .willReturn(listOf(projection1, projection2))

        // when
        val result = ideaCloudService.listRandomPublicIdeas()

        // then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("아이디어1", result[0].idea)
        assertEquals(100L, result[0].authorId)
        assertEquals("작성자1", result[0].authorName)
        verify(ideaCloudRepository).findRandomPublicIdeasWithAuthor()
    }

    @Test
    fun `listRandomPublicIdeas는 아이디어가 없으면 빈 목록을 반환한다`() {
        // given
        given(ideaCloudRepository.findRandomPublicIdeasWithAuthor()).willReturn(emptyList())

        // when
        val result = ideaCloudService.listRandomPublicIdeas()

        // then
        assertEquals(0, result.size)
        verify(ideaCloudRepository).findRandomPublicIdeasWithAuthor()
    }

    @Test
    fun `createIdea는 새 아이디어를 생성하고 반환한다`() {
        // given
        val authorId = 100L
        val idea = "새로운 아이디어"
        val savedIdeaCloud =
            IdeaCloud(
                id = 1L,
                idea = idea,
                author = authorId,
                state = IdeaCloudState.PUBLIC,
            )

        given(ideaCloudRepository.save(any<IdeaCloud>())).willReturn(savedIdeaCloud)

        // when
        val result = ideaCloudService.createIdea(authorId, idea)

        // then
        assertEquals(idea, result.idea)
        assertEquals(authorId, result.author)
        assertEquals(IdeaCloudState.PUBLIC, result.state)
        verify(ideaCloudRepository).save(any<IdeaCloud>())
    }

    @Test
    fun `deleteIdea는 존재하는 아이디어를 삭제한다`() {
        // given
        val ideaId = 1L
        val ideaCloud =
            IdeaCloud(
                id = ideaId,
                idea = "삭제할 아이디어",
                author = 100L,
                state = IdeaCloudState.PUBLIC,
            )
        given(ideaCloudRepository.findById(ideaId)).willReturn(Optional.of(ideaCloud))

        // when
        ideaCloudService.deleteIdea(ideaId)

        // then
        verify(ideaCloudRepository).findById(ideaId)
        verify(ideaCloudRepository).delete(ideaCloud)
    }

    @Test
    fun `deleteIdea는 존재하지 않는 아이디어 삭제 시 NotFoundException을 던진다`() {
        // given
        val ideaId = 999L
        given(ideaCloudRepository.findById(ideaId)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            ideaCloudService.deleteIdea(ideaId)
        }
    }

    private fun createMockProjection(
        id: Long,
        idea: String,
        authorId: Long,
        authorName: String,
    ): IdeaCloudWithAuthorProjection {
        val projection = mock<IdeaCloudWithAuthorProjection>()
        given(projection.id).willReturn(id)
        given(projection.idea).willReturn(idea)
        given(projection.authorId).willReturn(authorId)
        given(projection.authorName).willReturn(authorName)
        given(projection.createdAt).willReturn(LocalDateTime.now())
        return projection
    }
}
