package com.sight.service

import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingAnswerField
import com.sight.domain.groupmatching.GroupMatchingSubject
import com.sight.domain.groupmatching.MatchedGroup
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.MatchedGroupRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.LocalDateTime

class GroupMatchingAnswerServiceTest {
    private val answerRepository = mock<GroupMatchingAnswerRepository>()
    private val answerFieldRepository = mock<GroupMatchingAnswerFieldRepository>()
    private val subjectRepository = mock<GroupMatchingSubjectRepository>()
    private val matchedGroupRepository = mock<MatchedGroupRepository>()

    private val service =
        GroupMatchingAnswerService(
            answerRepository,
            answerFieldRepository,
            subjectRepository,
            matchedGroupRepository,
        )

    @Test
    fun `getAllAnswers는 응답이 없으면 빈 배열을 반환한다`() {
        // given
        val groupMatchingId = "gm-1"
        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId)

        // then
        assertEquals(0, result.answers.size)
        assertEquals(0, result.total)
    }

    @Test
    fun `getAllAnswers는 생성일시 기준 내림차순으로 정렬한다`() {
        // given
        val groupMatchingId = "gm-1"
        val now = LocalDateTime.now()
        val answer1 =
            GroupMatchingAnswer(
                id = "ans-1",
                userId = 1L,
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                groupMatchingId = groupMatchingId,
                createdAt = now.minusDays(1),
                updatedAt = now.minusDays(1),
            )
        val answer2 =
            GroupMatchingAnswer(
                id = "ans-2",
                userId = 2L,
                groupType = GroupCategory.PROJECT,
                isPreferOnline = false,
                groupMatchingId = groupMatchingId,
                createdAt = now,
                updatedAt = now,
            )

        // Repository는 이미 내림차순으로 정렬된 데이터 반환
        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(listOf(answer2, answer1))
        given(answerFieldRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(answerFieldRepository.findAllByAnswerId("ans-2")).willReturn(emptyList())
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(subjectRepository.findAllByAnswerId("ans-2")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-2")).willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId)

        // then
        assertEquals(2, result.answers.size)
        assertEquals("ans-2", result.answers[0].answerId) // 최신이 먼저
        assertEquals("ans-1", result.answers[1].answerId)
    }

    @Test
    fun `getAllAnswers는 selectedFields가 없으면 빈 배열을 반환한다`() {
        // given
        val groupMatchingId = "gm-1"
        val answer =
            GroupMatchingAnswer(
                id = "ans-1",
                userId = 1L,
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                groupMatchingId = groupMatchingId,
            )
        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(listOf(answer))
        given(answerFieldRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId)

        // then
        assertEquals(0, result.answers[0].selectedFields.size)
    }

    @Test
    fun `getAllAnswers는 subjectIdeas가 없으면 빈 배열을 반환한다`() {
        // given
        val groupMatchingId = "gm-1"
        val answer =
            GroupMatchingAnswer(
                id = "ans-1",
                userId = 1L,
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                groupMatchingId = groupMatchingId,
            )
        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(listOf(answer))
        given(answerFieldRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId)

        // then
        assertEquals(0, result.answers[0].subjectIdeas.size)
    }

    @Test
    fun `getAllAnswers는 matchedGroupIds가 없으면 빈 배열을 반환한다`() {
        // given
        val groupMatchingId = "gm-1"
        val answer =
            GroupMatchingAnswer(
                id = "ans-1",
                userId = 1L,
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                groupMatchingId = groupMatchingId,
            )
        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(listOf(answer))
        given(answerFieldRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId)

        // then
        assertEquals(0, result.answers[0].matchedGroupIds.size)
    }

    @Test
    fun `getAllAnswers는 연관 데이터를 포함하여 반환한다`() {
        // given
        val groupMatchingId = "gm-1"
        val answer =
            GroupMatchingAnswer(
                id = "ans-1",
                userId = 1L,
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                groupMatchingId = groupMatchingId,
            )
        val answerField1 =
            GroupMatchingAnswerField(
                id = "af-1",
                answerId = "ans-1",
                fieldId = "field-1",
            )
        val answerField2 =
            GroupMatchingAnswerField(
                id = "af-2",
                answerId = "ans-1",
                fieldId = "field-2",
            )
        val subject1 =
            GroupMatchingSubject(
                id = "subj-1",
                answerId = "ans-1",
                subject = "주제 1",
            )
        val matchedGroup =
            MatchedGroup(
                id = "mg-1",
                answerId = "ans-1",
                groupId = 100L,
            )

        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(listOf(answer))
        given(answerFieldRepository.findAllByAnswerId("ans-1"))
            .willReturn(listOf(answerField1, answerField2))
        given(subjectRepository.findAllByAnswerId("ans-1"))
            .willReturn(listOf(subject1))
        given(matchedGroupRepository.findAllByAnswerId("ans-1"))
            .willReturn(listOf(matchedGroup))

        // when
        val result = service.getAllAnswers(groupMatchingId)

        // then
        assertEquals(1, result.answers.size)
        assertEquals(2, result.answers[0].selectedFields.size)
        assertEquals("field-1", result.answers[0].selectedFields[0])
        assertEquals("field-2", result.answers[0].selectedFields[1])
        assertEquals(1, result.answers[0].subjectIdeas.size)
        assertEquals("주제 1", result.answers[0].subjectIdeas[0])
        assertEquals(1, result.answers[0].matchedGroupIds.size)
        assertEquals(100L, result.answers[0].matchedGroupIds[0])
    }
}
