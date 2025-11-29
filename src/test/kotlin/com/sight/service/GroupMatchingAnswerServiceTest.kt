package com.sight.service

import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingAnswerField
import com.sight.domain.groupmatching.GroupMatchingSubject
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingSubjectRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class GroupMatchingAnswerServiceTest {
    private val answerRepository = mock<GroupMatchingAnswerRepository>()
    private val fieldRepository = mock<GroupMatchingAnswerFieldRepository>()
    private val subjectRepository = mock<GroupMatchingSubjectRepository>()

    private val service =
        GroupMatchingAnswerService(
            answerRepository,
            fieldRepository,
            subjectRepository,
        )

    // 테스트 공통 데이터
    private val userId = 1L
    private val matchingId = "match-1"
    private val groupType = GroupCategory.STUDY

    @Test
    fun `이미 응답을 제출한 경우 Exception이 발생한다`() {
        // given
        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId))
            .willReturn(true)

        // when & then
        assertThrows<UnprocessableEntityException> {
            service.createGroupMatchingAnswer(
                groupType,
                true,
                userId,
                matchingId,
                emptyList(),
                emptyList(),
            )
        }

        verify(answerRepository, never()).save(any())
    }

    @Test
    fun `모든 데이터가 유효하면 정상적으로 저장된다`() {
        // given
        val subjects = listOf("Java", "Kotlin")
        val fields = listOf("Backend")

        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId))
            .willReturn(false)

        // save 호출 시 전달된 객체를 그대로 반환하도록 설정 (ID는 서비스 내부에서 생성됨)
        given(answerRepository.save(any<GroupMatchingAnswer>())).willAnswer { it.arguments[0] }
        given(subjectRepository.saveAll(any<List<GroupMatchingSubject>>())).willAnswer { it.arguments[0] }

        // when
        val result =
            service.createGroupMatchingAnswer(
                groupType = groupType,
                isPreferOnline = true,
                userId = userId,
                groupMatchingId = matchingId,
                groupMatchingFieldIds = fields,
                groupMatchingSubjects = subjects,
            )

        // then
        assertEquals(userId, result.answer.userId)
        assertEquals(subjects.size, result.subjectIds.size)

        verify(answerRepository).save(any())
        verify(fieldRepository).saveAll(any<List<GroupMatchingAnswerField>>())
        verify(subjectRepository).saveAll(any<List<GroupMatchingSubject>>())
    }

    @Test
    fun `subject가 없는 경우 subjectIds는 emptyList로 들어간다`() {
        // given
        val emptySubjects = emptyList<String>()

        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId))
            .willReturn(false)
        given(answerRepository.save(any<GroupMatchingAnswer>())).willAnswer { it.arguments[0] }

        // when
        val result =
            service.createGroupMatchingAnswer(
                groupType = groupType,
                isPreferOnline = true,
                userId = userId,
                groupMatchingId = matchingId,
                groupMatchingFieldIds = listOf("Backend"),
                groupMatchingSubjects = emptySubjects,
            )

        // then
        assertTrue(result.subjectIds.isEmpty())

        verify(answerRepository).save(any())
        verify(fieldRepository).saveAll(any<List<GroupMatchingAnswerField>>())
        // 핵심 검증: subjectRepository는 호출되지 않아야 함
        verify(subjectRepository, never()).saveAll(any<List<GroupMatchingSubject>>())
    }
}
