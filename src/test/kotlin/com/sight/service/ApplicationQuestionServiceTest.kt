package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.application.ApplicationQuestion
import com.sight.repository.ApplicationQuestionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ApplicationQuestionServiceTest {
    private val applicationQuestionRepository = mock<ApplicationQuestionRepository>()
    private val service = ApplicationQuestionService(applicationQuestionRepository)

    @Test
    fun `createQuestion은 비노출 상태와 순서 없음으로 문항을 생성한다`() {
        val captor = argumentCaptor<ApplicationQuestion>()
        given(applicationQuestionRepository.save(any<ApplicationQuestion>())).willAnswer { it.arguments[0] }

        val result =
            service.createQuestion(
                title = "자기소개",
                description = "자기소개를 작성해주세요.",
                minLength = 100,
            )

        verify(applicationQuestionRepository).save(captor.capture())
        assertEquals(result, captor.firstValue)
        assertEquals("자기소개", result.title)
        assertEquals("자기소개를 작성해주세요.", result.description)
        assertEquals(100, result.minLength)
        assertEquals(null, result.order)
        assertFalse(result.isExposed)
    }

    @Test
    fun `listQuestionsByIds는 요청한 문항 순서대로 반환한다`() {
        val firstQuestion = question(id = "question-1", title = "첫 문항")
        val secondQuestion = question(id = "question-2", title = "둘째 문항")
        given(applicationQuestionRepository.findAllById(listOf("question-2", "question-1")))
            .willReturn(listOf(firstQuestion, secondQuestion))

        val result = service.listQuestionsByIds(listOf("question-2", "question-1"))

        assertEquals(listOf("question-2", "question-1"), result.map { it.id })
    }

    @Test
    fun `listQuestionsByIds는 존재하지 않는 문항이 있으면 NotFoundException을 던진다`() {
        given(applicationQuestionRepository.findAllById(listOf("question-1", "missing-question")))
            .willReturn(listOf(question(id = "question-1", title = "첫 문항")))

        assertThrows<NotFoundException> {
            service.listQuestionsByIds(listOf("question-1", "missing-question"))
        }
    }

    @Test
    fun `listAllQuestions는 저장된 모든 문항을 반환한다`() {
        val questions = listOf(question(id = "question-1", title = "첫 문항"))
        given(applicationQuestionRepository.findAll()).willReturn(questions)

        val result = service.listAllQuestions()

        assertEquals(questions, result)
    }

    @Test
    fun `updateQuestions는 노출 여부와 순서가 유효하면 문항을 수정한다`() {
        val question = question(id = "question-1", title = "기존 문항")
        given(applicationQuestionRepository.findAllById(setOf("question-1"))).willReturn(listOf(question))

        service.updateQuestions(
            listOf(
                UpdateApplicationQuestionCommand(
                    id = "question-1",
                    title = "수정 문항",
                    description = "수정 설명",
                    minLength = 10,
                    order = 1,
                    isExposed = true,
                ),
            ),
        )

        assertEquals("수정 문항", question.title)
        assertEquals("수정 설명", question.description)
        assertEquals(10, question.minLength)
        assertEquals(1, question.order)
        assertTrue(question.isExposed)
        verify(applicationQuestionRepository).saveAll(listOf(question))
    }

    @Test
    fun `updateQuestions는 노출 문항 순서가 연속되지 않으면 BadRequestException을 던진다`() {
        assertThrows<BadRequestException> {
            service.updateQuestions(
                listOf(
                    UpdateApplicationQuestionCommand(
                        id = "question-1",
                        title = "문항",
                        description = "설명",
                        minLength = 0,
                        order = 2,
                        isExposed = true,
                    ),
                ),
            )
        }
    }

    private fun question(
        id: String,
        title: String,
    ): ApplicationQuestion {
        return ApplicationQuestion(
            id = id,
            title = title,
            description = "설명",
            minLength = 0,
            order = null,
            isExposed = false,
        )
    }
}
