package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.ActivityFrequency
import com.sight.domain.groupmatching.GroupMatching
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingAnswerOption
import com.sight.domain.groupmatching.GroupMatchingOption
import com.sight.domain.groupmatching.GroupMatchingType
import com.sight.repository.GroupMatchingAnswerOptionRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingOptionRepository
import com.sight.repository.GroupMatchingRepository
import com.sight.repository.MatchedGroupRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.Optional

class GroupMatchingAnswerServiceTest {
    private val answerRepository = mock<GroupMatchingAnswerRepository>()
    private val answerOptionRepository = mock<GroupMatchingAnswerOptionRepository>()
    private val matchedGroupRepository = mock<MatchedGroupRepository>()
    private val optionRepository = mock<GroupMatchingOptionRepository>()
    private val groupMatchingRepository = mock<GroupMatchingRepository>()

    private val service =
        GroupMatchingAnswerService(
            answerRepository,
            answerOptionRepository,
            matchedGroupRepository,
            optionRepository,
            groupMatchingRepository,
        )

    private val userId = 1L
    private val matchingId = "match-1"
    private val groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY

    @Test
    fun `존재하지 않는 그룹 매칭 ID인 경우 NotFoundException이 발생한다`() {
        // given
        given(groupMatchingRepository.findById(matchingId))
            .willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            service.createGroupMatchingAnswer(
                groupType = groupType,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "온라인 스터디",
                otherSuggestions = null,
                userId = userId,
                groupMatchingId = matchingId,
                selectedOptionIds = emptyList(),
                customOption = null,
                role = null,
                hasIdea = null,
                idea = null,
            )
        }
    }

    @Test
    fun `마감 기한이 지난 경우 UnprocessableEntityException이 발생한다`() {
        // given
        val closedMatching = mock<GroupMatching>()
        given(closedMatching.closedAt).willReturn(LocalDateTime.now().minusDays(1))

        given(groupMatchingRepository.findById(matchingId))
            .willReturn(Optional.of(closedMatching))

        // when & then
        val exception =
            assertThrows<UnprocessableEntityException> {
                service.createGroupMatchingAnswer(
                    groupType = groupType,
                    isPreferOnline = true,
                    activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                    activityFormat = "온라인 스터디",
                    otherSuggestions = null,
                    userId = userId,
                    groupMatchingId = matchingId,
                    selectedOptionIds = emptyList(),
                    customOption = null,
                    role = null,
                    hasIdea = null,
                    idea = null,
                )
            }

        assertTrue(exception.message.contains("마감되었습니다"))
    }

    @Test
    fun `이미 응답을 제출한 경우 UnprocessableEntityException이 발생한다`() {
        // given
        val validMatching = mock<GroupMatching>()
        given(validMatching.closedAt).willReturn(LocalDateTime.now().plusDays(1))
        given(groupMatchingRepository.findById(matchingId)).willReturn(Optional.of(validMatching))
        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId))
            .willReturn(true)

        // when & then
        assertThrows<UnprocessableEntityException> {
            service.createGroupMatchingAnswer(
                groupType = groupType,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "온라인 스터디",
                otherSuggestions = null,
                userId = userId,
                groupMatchingId = matchingId,
                selectedOptionIds = emptyList(),
                customOption = null,
                role = null,
                hasIdea = null,
                idea = null,
            )
        }

        verify(answerRepository, never()).save(any())
    }

    @Test
    fun `모든 데이터가 유효하면 정상적으로 저장된다`() {
        // given
        val optionId = "opt-1"
        val option =
            GroupMatchingOption(
                id = optionId,
                groupMatchingId = matchingId,
                name = "Java",
                groupMatchingType = groupType,
            )

        val validMatching = mock<GroupMatching>()
        given(validMatching.closedAt).willReturn(LocalDateTime.now().plusDays(1))
        given(groupMatchingRepository.findById(matchingId)).willReturn(Optional.of(validMatching))
        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId))
            .willReturn(false)
        given(optionRepository.findAllById(listOf(optionId))).willReturn(listOf(option))
        given(answerRepository.save(any<GroupMatchingAnswer>())).willAnswer { it.arguments[0] }

        // when
        val result =
            service.createGroupMatchingAnswer(
                groupType = groupType,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.THREE_OR_FOUR,
                activityFormat = "주 2회 온라인 스터디",
                otherSuggestions = "없음",
                userId = userId,
                groupMatchingId = matchingId,
                selectedOptionIds = listOf(optionId),
                customOption = null,
                role = null,
                hasIdea = null,
                idea = null,
            )

        // then
        assertEquals(userId, result.answer.userId)
        assertEquals(1, result.options.size)
        assertEquals(optionId, result.options[0].id)

        verify(answerRepository).save(any())
        verify(answerOptionRepository).saveAll(any<List<GroupMatchingAnswerOption>>())
    }

    @Test
    fun `옵션 없이도 정상적으로 저장된다`() {
        // given
        val validMatching = mock<GroupMatching>()
        given(validMatching.closedAt).willReturn(LocalDateTime.now().plusDays(1))
        given(groupMatchingRepository.findById(matchingId)).willReturn(Optional.of(validMatching))
        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId))
            .willReturn(false)
        given(answerRepository.save(any<GroupMatchingAnswer>())).willAnswer { it.arguments[0] }

        // when
        val result =
            service.createGroupMatchingAnswer(
                groupType = GroupMatchingType.PRACTICAL_PROJECT,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.FIVE_TO_SEVEN,
                activityFormat = "매일 온라인 미팅",
                otherSuggestions = null,
                userId = userId,
                groupMatchingId = matchingId,
                selectedOptionIds = emptyList(),
                customOption = null,
                role = "백엔드 개발",
                hasIdea = true,
                idea = "웹 앱 만들기",
            )

        // then
        assertTrue(result.options.isEmpty())
        verify(answerRepository).save(any())
    }

    @Test
    fun `존재하지 않는 옵션 ID가 포함된 경우 BadRequestException이 발생한다`() {
        // given
        val invalidOptionId = "invalid-opt"

        val validMatching = mock<GroupMatching>()
        given(validMatching.closedAt).willReturn(LocalDateTime.now().plusDays(1))
        given(groupMatchingRepository.findById(matchingId)).willReturn(Optional.of(validMatching))
        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId)).willReturn(false)
        given(optionRepository.findAllById(listOf(invalidOptionId))).willReturn(emptyList())

        // when & then
        assertThrows<BadRequestException> {
            service.createGroupMatchingAnswer(
                groupType = groupType,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "온라인 스터디",
                otherSuggestions = null,
                userId = userId,
                groupMatchingId = matchingId,
                selectedOptionIds = listOf(invalidOptionId),
                customOption = null,
                role = null,
                hasIdea = null,
                idea = null,
            )
        }

        verify(answerOptionRepository, never()).saveAll(any<List<GroupMatchingAnswerOption>>())
    }

    @Test
    fun `listAnswers는 optionId가 존재하지 않으면 에러를 던진다`() {
        // given
        val groupMatchingId = "gm-1"
        val invalidOptionId = "invalid-option"
        given(optionRepository.findById(invalidOptionId)).willReturn(Optional.empty())

        // when & then
        assertThrows<BadRequestException> {
            service.listAnswers(groupMatchingId, optionId = invalidOptionId, offset = 0, limit = 20)
        }
    }

    @Test
    fun `중복된 옵션 ID가 포함된 경우 BadRequestException이 발생한다`() {
        // given
        val validMatching = mock<GroupMatching>()
        given(validMatching.closedAt).willReturn(LocalDateTime.now().plusDays(1))
        given(groupMatchingRepository.findById(matchingId)).willReturn(Optional.of(validMatching))
        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId)).willReturn(false)

        // when & then
        assertThrows<BadRequestException> {
            service.createGroupMatchingAnswer(
                groupType = groupType,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "온라인 스터디",
                otherSuggestions = null,
                userId = userId,
                groupMatchingId = matchingId,
                selectedOptionIds = listOf("opt-1", "opt-1"),
                customOption = null,
                role = null,
                hasIdea = null,
                idea = null,
            )
        }
    }
}
