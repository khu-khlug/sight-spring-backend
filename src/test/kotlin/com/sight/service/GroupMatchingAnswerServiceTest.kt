package com.sight.service

import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatching
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingAnswerField
import com.sight.domain.groupmatching.GroupMatchingSubject
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingRepository
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
import java.time.LocalDateTime
import java.util.Optional

class GroupMatchingAnswerServiceTest {
    private val answerRepository = mock<GroupMatchingAnswerRepository>()
    private val fieldRepository = mock<GroupMatchingAnswerFieldRepository>()
    private val fieldMetaRepository = mock<GroupMatchingFieldRepository>()
    private val subjectRepository = mock<GroupMatchingSubjectRepository>()
    private val groupMatchingRepository = mock<GroupMatchingRepository>()

    private val service =
        GroupMatchingAnswerService(
            answerRepository,
            fieldRepository,
            fieldMetaRepository,
            subjectRepository,
            groupMatchingRepository,
        )

    // 테스트 공통 데이터
    private val userId = 1L
    private val matchingId = "match-1"
    private val groupType = GroupCategory.STUDY

    @Test
    fun `존재하지 않는 그룹 매칭 ID인 경우 NotFoundException이 발생한다`() {
        // given
        // DB에서 조회되지 않음 (Empty Optional)

        given(groupMatchingRepository.findById(matchingId))
            .willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            service.createGroupMatchingAnswer(
                groupType,
                true,
                userId,
                matchingId,
                emptyList(),
                emptyList(),
            )
        }
    }

    // [New] 테스트 2: 마감 기한이 지난 경우
    @Test
    fun `마감 기한이 지난 경우 UnprocessableEntityException이 발생한다`() {
        // given
        // 마감 시간이 "어제"인 GroupMatching Mock 생성
        val closedMatching = mock<GroupMatching>()
        given(closedMatching.closedAt).willReturn(LocalDateTime.now().minusDays(1))

        // 해당 매칭이 조회된다고 가정
        given(groupMatchingRepository.findById(matchingId))
            .willReturn(Optional.of(closedMatching))

        // when & then
        val exception =
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

        // 에러 메시지 검증 (선택 사항)
        assertTrue(exception.message?.contains("마감되었습니다") == true)
    }

    @Test
    fun `이미 응답을 제출한 경우 Exception이 발생한다`() {
        // given
        val validMatching = mock<GroupMatching>()
        given(validMatching.closedAt).willReturn(LocalDateTime.now().plusDays(1))
        given(groupMatchingRepository.findById(matchingId)).willReturn(Optional.of(validMatching))
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

        val validMatching = mock<GroupMatching>()
        given(validMatching.closedAt).willReturn(LocalDateTime.now().plusDays(1))
        given(groupMatchingRepository.findById(matchingId)).willReturn(Optional.of(validMatching))
        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId))
            .willReturn(false)
        given(fieldMetaRepository.existsById(any())).willReturn(true)
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
        verify(fieldMetaRepository).existsById("Backend")
    }

    @Test
    fun `subject가 없는 경우 subjectIds는 emptyList로 들어간다`() {
        // given
        val emptySubjects = emptyList<String>()

        val validMatching = mock<GroupMatching>()
        given(validMatching.closedAt).willReturn(LocalDateTime.now().plusDays(1))
        given(groupMatchingRepository.findById(matchingId)).willReturn(Optional.of(validMatching))

        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId))
            .willReturn(false)
        given(fieldMetaRepository.existsById(any())).willReturn(true)
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

    @Test
    fun `존재하지 않는 필드 ID가 포함된 경우 UnprocessableEntityException이 발생한다`() {
        // given
        val invalidFields = listOf("InvalidFieldID")

        val validMatching = mock<GroupMatching>()
        given(validMatching.closedAt).willReturn(LocalDateTime.now().plusDays(1))
        given(groupMatchingRepository.findById(matchingId)).willReturn(Optional.of(validMatching))

        given(answerRepository.existsByUserIdAndGroupMatchingId(userId, matchingId))
            .willReturn(false)

        // [Added] 필드 ID가 존재하지 않음(false)을 리턴하도록 설정
        given(fieldMetaRepository.existsById("InvalidFieldID")).willReturn(false)

        // Answer 저장을 위해 필요한 mock 설정 (Service 코드 순서상 Answer 저장 후 필드 검사함)
        given(answerRepository.save(any<GroupMatchingAnswer>())).willAnswer {
            (it.arguments[0] as GroupMatchingAnswer).copy(id = "temp-id")
        }

        // when & then
        assertThrows<UnprocessableEntityException> {
            service.createGroupMatchingAnswer(
                groupType = groupType,
                isPreferOnline = true,
                userId = userId,
                groupMatchingId = matchingId,
                groupMatchingFieldIds = invalidFields,
                groupMatchingSubjects = emptyList(),
            )
        }

        // [Added] 필드 검증 메서드가 호출되었는지 확인
        verify(fieldMetaRepository).existsById("InvalidFieldID")
        // 예외 발생 시 중간 테이블(AnswerField) 저장은 호출되지 않아야 함 (map 내부에서 예외 발생)
        verify(fieldRepository, never()).saveAll(any<List<GroupMatchingAnswerField>>())
    }
}
