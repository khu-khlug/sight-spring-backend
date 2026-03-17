package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.groupmatching.ActivityFrequency
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingType
import com.sight.domain.groupmatching.ProvisionalGroup
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingRepository
import com.sight.repository.ProvisionalGroupRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Instant
import java.util.Optional

class ProvisionalGroupServiceTest {
    private val provisionalGroupRepository = mock<ProvisionalGroupRepository>()
    private val groupMatchingRepository = mock<GroupMatchingRepository>()
    private val answerRepository = mock<GroupMatchingAnswerRepository>()

    private lateinit var service: ProvisionalGroupService

    private val groupMatchingId = "gm-1"
    private val provisionalGroupId = "pg-1"

    @BeforeEach
    fun setUp() {
        service = ProvisionalGroupService(provisionalGroupRepository, groupMatchingRepository, answerRepository)
    }

    @Test
    fun `listProvisionalGroups - 그룹 매칭이 존재하지 않으면 NotFoundException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(false)

        // when & then
        assertThrows<NotFoundException> {
            service.listProvisionalGroups(groupMatchingId)
        }
    }

    @Test
    fun `listProvisionalGroups - 가상 그룹 목록을 반환한다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)

        val group =
            ProvisionalGroup(
                id = provisionalGroupId,
                name = "그룹A",
                groupMatchingId = groupMatchingId,
                createdAt = Instant.now(),
            )
        given(provisionalGroupRepository.findAllByGroupMatchingId(groupMatchingId))
            .willReturn(listOf(group))
        given(answerRepository.countByProvisionalGroupId(provisionalGroupId))
            .willReturn(3L)

        // when
        val result = service.listProvisionalGroups(groupMatchingId)

        // then
        assertEquals(1, result.provisionalGroups.size)
        assertEquals("그룹A", result.provisionalGroups[0].name)
        assertEquals(3L, result.provisionalGroups[0].answerCount)
    }

    @Test
    fun `createProvisionalGroup - 그룹 매칭이 존재하지 않으면 NotFoundException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(false)

        // when & then
        assertThrows<NotFoundException> {
            service.createProvisionalGroup(groupMatchingId, "그룹A")
        }
    }

    @Test
    fun `createProvisionalGroup - 정상적으로 가상 그룹을 생성한다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)
        given(provisionalGroupRepository.save(any<ProvisionalGroup>())).willAnswer { it.arguments[0] }

        // when
        val result = service.createProvisionalGroup(groupMatchingId, "그룹A")

        // then
        assertEquals("그룹A", result.name)
        assertEquals(groupMatchingId, result.groupMatchingId)
        verify(provisionalGroupRepository).save(any())
    }

    @Test
    fun `updateProvisionalGroup - 가상 그룹이 존재하지 않으면 NotFoundException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)
        given(provisionalGroupRepository.findById(provisionalGroupId)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            service.updateProvisionalGroup(groupMatchingId, provisionalGroupId, "새이름")
        }
    }

    @Test
    fun `updateProvisionalGroup - 다른 그룹 매칭에 속한 가상 그룹이면 BadRequestException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)

        val group =
            ProvisionalGroup(
                id = provisionalGroupId,
                name = "그룹A",
                groupMatchingId = "other-gm",
            )
        given(provisionalGroupRepository.findById(provisionalGroupId))
            .willReturn(Optional.of(group))

        // when & then
        assertThrows<BadRequestException> {
            service.updateProvisionalGroup(groupMatchingId, provisionalGroupId, "새이름")
        }
    }

    @Test
    fun `updateProvisionalGroup - 정상적으로 이름을 변경한다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)

        val group =
            ProvisionalGroup(
                id = provisionalGroupId,
                name = "그룹A",
                groupMatchingId = groupMatchingId,
            )
        given(provisionalGroupRepository.findById(provisionalGroupId))
            .willReturn(Optional.of(group))
        given(provisionalGroupRepository.save(any<ProvisionalGroup>())).willAnswer { it.arguments[0] }
        given(answerRepository.countByProvisionalGroupId(provisionalGroupId)).willReturn(2L)

        // when
        val result = service.updateProvisionalGroup(groupMatchingId, provisionalGroupId, "새이름")

        // then
        assertEquals("새이름", result.name)
        assertEquals(2L, result.answerCount)
    }

    @Test
    fun `deleteProvisionalGroup - 가상 그룹이 존재하지 않으면 NotFoundException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)
        given(provisionalGroupRepository.findById(provisionalGroupId)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            service.deleteProvisionalGroup(groupMatchingId, provisionalGroupId)
        }
    }

    @Test
    fun `deleteProvisionalGroup - 다른 그룹 매칭에 속한 가상 그룹이면 BadRequestException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)

        val group =
            ProvisionalGroup(
                id = provisionalGroupId,
                name = "그룹A",
                groupMatchingId = "other-gm",
            )
        given(provisionalGroupRepository.findById(provisionalGroupId))
            .willReturn(Optional.of(group))

        // when & then
        assertThrows<BadRequestException> {
            service.deleteProvisionalGroup(groupMatchingId, provisionalGroupId)
        }
    }

    @Test
    fun `deleteProvisionalGroup - 삭제 시 연관된 답변의 provisionalGroupId를 null로 업데이트한다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)

        val group =
            ProvisionalGroup(
                id = provisionalGroupId,
                name = "그룹A",
                groupMatchingId = groupMatchingId,
            )
        given(provisionalGroupRepository.findById(provisionalGroupId))
            .willReturn(Optional.of(group))

        // when
        service.deleteProvisionalGroup(groupMatchingId, provisionalGroupId)

        // then
        verify(answerRepository).updateProvisionalGroupIdToNullByProvisionalGroupId(provisionalGroupId)
        verify(provisionalGroupRepository).delete(group)
    }

    @Test
    fun `assignProvisionalGroup - 그룹 매칭이 존재하지 않으면 NotFoundException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(false)

        // when & then
        assertThrows<NotFoundException> {
            service.assignProvisionalGroup(groupMatchingId, "answer-1", provisionalGroupId)
        }
    }

    @Test
    fun `assignProvisionalGroup - 답변이 존재하지 않으면 NotFoundException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)
        given(answerRepository.findById("answer-1")).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            service.assignProvisionalGroup(groupMatchingId, "answer-1", provisionalGroupId)
        }
    }

    @Test
    fun `assignProvisionalGroup - 답변이 다른 그룹 매칭에 속하면 BadRequestException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)

        val answer =
            GroupMatchingAnswer(
                id = "answer-1",
                userId = 1L,
                groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "온라인",
                groupMatchingId = "other-gm",
            )
        given(answerRepository.findById("answer-1")).willReturn(Optional.of(answer))

        // when & then
        assertThrows<BadRequestException> {
            service.assignProvisionalGroup(groupMatchingId, "answer-1", provisionalGroupId)
        }
    }

    @Test
    fun `assignProvisionalGroup - 가상 그룹이 다른 그룹 매칭에 속하면 BadRequestException을 던진다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)

        val answer =
            GroupMatchingAnswer(
                id = "answer-1",
                userId = 1L,
                groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "온라인",
                groupMatchingId = groupMatchingId,
            )
        given(answerRepository.findById("answer-1")).willReturn(Optional.of(answer))

        val group =
            ProvisionalGroup(
                id = provisionalGroupId,
                name = "그룹A",
                groupMatchingId = "other-gm",
            )
        given(provisionalGroupRepository.findById(provisionalGroupId))
            .willReturn(Optional.of(group))

        // when & then
        assertThrows<BadRequestException> {
            service.assignProvisionalGroup(groupMatchingId, "answer-1", provisionalGroupId)
        }
    }

    @Test
    fun `assignProvisionalGroup - 정상적으로 가상 그룹을 할당한다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)

        val answer =
            GroupMatchingAnswer(
                id = "answer-1",
                userId = 1L,
                groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "온라인",
                groupMatchingId = groupMatchingId,
            )
        given(answerRepository.findById("answer-1")).willReturn(Optional.of(answer))

        val group =
            ProvisionalGroup(
                id = provisionalGroupId,
                name = "그룹A",
                groupMatchingId = groupMatchingId,
            )
        given(provisionalGroupRepository.findById(provisionalGroupId))
            .willReturn(Optional.of(group))
        given(answerRepository.save(any<GroupMatchingAnswer>())).willAnswer { it.arguments[0] }

        // when
        service.assignProvisionalGroup(groupMatchingId, "answer-1", provisionalGroupId)

        // then
        verify(answerRepository).save(any())
    }

    @Test
    fun `assignProvisionalGroup - provisionalGroupId가 null이면 할당을 해제한다`() {
        // given
        given(groupMatchingRepository.existsById(groupMatchingId)).willReturn(true)

        val answer =
            GroupMatchingAnswer(
                id = "answer-1",
                userId = 1L,
                groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "온라인",
                groupMatchingId = groupMatchingId,
                provisionalGroupId = provisionalGroupId,
            )
        given(answerRepository.findById("answer-1")).willReturn(Optional.of(answer))
        given(answerRepository.save(any<GroupMatchingAnswer>())).willAnswer { it.arguments[0] }

        // when
        service.assignProvisionalGroup(groupMatchingId, "answer-1", null)

        // then
        verify(answerRepository).save(any())
    }
}
