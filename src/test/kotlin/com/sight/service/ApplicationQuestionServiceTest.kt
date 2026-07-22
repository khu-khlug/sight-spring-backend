package com.sight.service

import com.sight.domain.application.ApplicationQuestion
import com.sight.repository.ApplicationQuestionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
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
}
