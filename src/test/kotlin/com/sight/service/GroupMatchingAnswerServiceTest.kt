package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingAnswerField
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.MatchedGroupRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import java.time.LocalDateTime

class GroupMatchingAnswerServiceTest {
    private val answerRepository = mock<GroupMatchingAnswerRepository>()
    private val answerFieldRepository = mock<GroupMatchingAnswerFieldRepository>()
    private val subjectRepository = mock<GroupMatchingSubjectRepository>()
    private val matchedGroupRepository = mock<MatchedGroupRepository>()
    private val fieldRepository = mock<GroupMatchingFieldRepository>()

    private val service =
        GroupMatchingAnswerService(
            answerRepository,
            answerFieldRepository,
            subjectRepository,
            matchedGroupRepository,
            fieldRepository,
        )

    private fun mockAnswerField(
        answerId: String,
        fieldId: String,
    ) = GroupMatchingAnswerField(
        id = "af-$answerId-$fieldId",
        answerId = answerId,
        fieldId = fieldId,
    )

    // === 기본 기능 테스트 ===

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

        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(listOf(answer2, answer1)) // 이미 내림차순
        given(answerFieldRepository.findAllByAnswerId("ans-1")).willReturn(listOf(mockAnswerField("ans-1", "field-1")))
        given(answerFieldRepository.findAllByAnswerId("ans-2")).willReturn(listOf(mockAnswerField("ans-2", "field-1")))
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(subjectRepository.findAllByAnswerId("ans-2")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-2")).willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId)

        // then
        assertEquals(2, result.answers.size)
        assertEquals("ans-2", result.answers[0].answerId)
        assertEquals("ans-1", result.answers[1].answerId)
    }

    @Test
    fun `getAllAnswers는 selectedFields가 없으면 에러를 던진다`() {
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

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId)
        }
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
        given(answerFieldRepository.findAllByAnswerId("ans-1")).willReturn(listOf(mockAnswerField("ans-1", "field-1")))
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
        given(answerFieldRepository.findAllByAnswerId("ans-1")).willReturn(listOf(mockAnswerField("ans-1", "field-1")))
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId)

        // then
        assertEquals(0, result.answers[0].matchedGroupIds.size)
    }

    // === groupType 필터링 테스트 ===

    @Test
    fun `getAllAnswers는 groupType이 STUDY나 PROJECT가 아니면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, groupType = "INVALID")
        }
    }

    @Test
    fun `getAllAnswers는 GroupCategory에 속하지만 groupType이 아닌 값은 거부한다`() {
        // given
        val groupMatchingId = "gm-1"

        // when & then - MANAGE, DOCUMENTATION 등은 GroupCategory지만 groupType은 아님
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, groupType = "MANAGE")
        }

        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, groupType = "DOCUMENTATION")
        }
    }

    @Test
    fun `getAllAnswers는 groupType 필터링이 동작한다`() {
        // given
        val groupMatchingId = "gm-1"
        val studyAnswer =
            GroupMatchingAnswer(
                id = "ans-1",
                userId = 1L,
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                groupMatchingId = groupMatchingId,
            )
        val projectAnswer =
            GroupMatchingAnswer(
                id = "ans-2",
                userId = 2L,
                groupType = GroupCategory.PROJECT,
                isPreferOnline = false,
                groupMatchingId = groupMatchingId,
            )

        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(listOf(studyAnswer, projectAnswer))
        given(answerFieldRepository.findAllByAnswerId("ans-1")).willReturn(listOf(mockAnswerField("ans-1", "field-1")))
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId, groupType = "STUDY")

        // then
        assertEquals(1, result.answers.size)
        assertEquals("ans-1", result.answers[0].answerId)
        assertEquals(GroupCategory.STUDY, result.answers[0].groupType)
        assertEquals(1, result.total) // 필터링 후 개수
    }

    // === fieldId 필터링 테스트 ===

    @Test
    fun `getAllAnswers는 fieldId가 유효하지 않으면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"
        val invalidFieldId = "invalid-field"
        given(fieldRepository.existsById(invalidFieldId)).willReturn(false)

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, fieldId = invalidFieldId)
        }
    }

    @Test
    fun `getAllAnswers는 fieldId 필터링이 동작한다`() {
        // given
        val groupMatchingId = "gm-1"
        val fieldId1 = "field-1"
        val fieldId2 = "field-2"
        val answer1 =
            GroupMatchingAnswer(
                id = "ans-1",
                userId = 1L,
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                groupMatchingId = groupMatchingId,
            )
        val answer2 =
            GroupMatchingAnswer(
                id = "ans-2",
                userId = 2L,
                groupType = GroupCategory.PROJECT,
                isPreferOnline = false,
                groupMatchingId = groupMatchingId,
            )

        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(listOf(answer1, answer2))
        given(fieldRepository.existsById(fieldId1)).willReturn(true)
        given(answerFieldRepository.findAllByAnswerId("ans-1"))
            .willReturn(listOf(mockAnswerField("ans-1", fieldId1)))
        given(answerFieldRepository.findAllByAnswerId("ans-2"))
            .willReturn(listOf(mockAnswerField("ans-2", fieldId2)))
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId, fieldId = fieldId1)

        // then
        assertEquals(1, result.answers.size)
        assertEquals("ans-1", result.answers[0].answerId)
        assertEquals(1, result.total) // fieldId1을 가진 답변만
    }

    @Test
    fun `getAllAnswers는 fieldId와 groupType을 함께 적용한다`() {
        // given
        val groupMatchingId = "gm-1"
        val fieldId = "field-1"
        val answers =
            listOf(
                GroupMatchingAnswer(
                    id = "ans-1",
                    userId = 1L,
                    groupType = GroupCategory.STUDY,
                    isPreferOnline = true,
                    groupMatchingId = groupMatchingId,
                ),
                GroupMatchingAnswer(
                    id = "ans-2",
                    userId = 2L,
                    groupType = GroupCategory.PROJECT,
                    isPreferOnline = false,
                    groupMatchingId = groupMatchingId,
                ),
                GroupMatchingAnswer(
                    id = "ans-3",
                    userId = 3L,
                    groupType = GroupCategory.STUDY,
                    isPreferOnline = true,
                    groupMatchingId = groupMatchingId,
                ),
            )

        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(answers)
        given(fieldRepository.existsById(fieldId)).willReturn(true)
        // ans-1: STUDY + field-1 (✓)
        given(answerFieldRepository.findAllByAnswerId("ans-1"))
            .willReturn(listOf(mockAnswerField("ans-1", fieldId)))
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        // ans-2: PROJECT + field-1 (✗ - groupType 불일치)
        given(answerFieldRepository.findAllByAnswerId("ans-2"))
            .willReturn(listOf(mockAnswerField("ans-2", fieldId)))
        // ans-3: STUDY + field-2 (✗ - fieldId 불일치)
        given(answerFieldRepository.findAllByAnswerId("ans-3"))
            .willReturn(listOf(mockAnswerField("ans-3", "field-2")))

        // when
        val result = service.getAllAnswers(groupMatchingId, groupType = "STUDY", fieldId = fieldId)

        // then
        assertEquals(1, result.answers.size)
        assertEquals("ans-1", result.answers[0].answerId)
        assertEquals(GroupCategory.STUDY, result.answers[0].groupType)
        assertEquals(1, result.total) // STUDY이면서 field-1을 가진 답변만
    }

    // === 페이지네이션 테스트 ===

    @Test
    fun `getAllAnswers는 offset이 음수이면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, offset = -1)
        }
    }

    @Test
    fun `getAllAnswers는 limit이 0 이하이면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"

        // when & then
        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, limit = 0)
        }

        assertThrows<BadRequestException> {
            service.getAllAnswers(groupMatchingId, limit = -1)
        }
    }

    @Test
    fun `getAllAnswers는 페이지네이션이 동작한다`() {
        // given
        val groupMatchingId = "gm-1"
        val answers =
            (1..5).map { i ->
                GroupMatchingAnswer(
                    id = "ans-$i",
                    userId = i.toLong(),
                    groupType = GroupCategory.STUDY,
                    isPreferOnline = true,
                    groupMatchingId = groupMatchingId,
                )
            }

        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(answers)
        answers.forEach { answer ->
            given(answerFieldRepository.findAllByAnswerId(answer.id)).willReturn(listOf(mockAnswerField(answer.id, "field-1")))
            given(subjectRepository.findAllByAnswerId(answer.id)).willReturn(emptyList())
            given(matchedGroupRepository.findAllByAnswerId(answer.id)).willReturn(emptyList())
        }

        // when
        val result = service.getAllAnswers(groupMatchingId, offset = 1, limit = 2)

        // then
        assertEquals(2, result.answers.size)
        assertEquals("ans-2", result.answers[0].answerId)
        assertEquals("ans-3", result.answers[1].answerId)
        assertEquals(5, result.total)
    }

    @Test
    fun `getAllAnswers는 offset이 total 이상이면 빈 배열을 반환한다`() {
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

        // when
        val result = service.getAllAnswers(groupMatchingId, offset = 5)

        // then
        assertEquals(0, result.answers.size)
        assertEquals(1, result.total)
    }

    @Test
    fun `getAllAnswers는 groupType과 페이지네이션을 함께 적용한다`() {
        // given
        val groupMatchingId = "gm-1"
        val answers =
            listOf(
                GroupMatchingAnswer(
                    id = "ans-1",
                    userId = 1L,
                    groupType = GroupCategory.STUDY,
                    isPreferOnline = true,
                    groupMatchingId = groupMatchingId,
                ),
                GroupMatchingAnswer(
                    id = "ans-2",
                    userId = 2L,
                    groupType = GroupCategory.PROJECT,
                    isPreferOnline = false,
                    groupMatchingId = groupMatchingId,
                ),
                GroupMatchingAnswer(
                    id = "ans-3",
                    userId = 3L,
                    groupType = GroupCategory.STUDY,
                    isPreferOnline = true,
                    groupMatchingId = groupMatchingId,
                ),
            )

        given(answerRepository.findAllByGroupMatchingIdOrderByCreatedAtDesc(groupMatchingId))
            .willReturn(answers)
        given(answerFieldRepository.findAllByAnswerId("ans-1")).willReturn(listOf(mockAnswerField("ans-1", "field-1")))
        given(subjectRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-1")).willReturn(emptyList())
        given(answerFieldRepository.findAllByAnswerId("ans-3")).willReturn(listOf(mockAnswerField("ans-3", "field-1")))
        given(subjectRepository.findAllByAnswerId("ans-3")).willReturn(emptyList())
        given(matchedGroupRepository.findAllByAnswerId("ans-3")).willReturn(emptyList())

        // when
        val result = service.getAllAnswers(groupMatchingId, groupType = "STUDY", offset = 0, limit = 1)

        // then
        assertEquals(1, result.answers.size)
        assertEquals("ans-1", result.answers[0].answerId)
        assertEquals(2, result.total) // STUDY만 2개
    }
}
