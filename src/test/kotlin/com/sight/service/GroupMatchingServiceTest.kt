package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatching
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.MatchedGroup
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.repository.projection.GroupWithMemberProjection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupMatchingServiceTest {
    private val groupMatchingAnswerRepository: GroupMatchingAnswerRepository = mock()
    private val matchedGroupRepository: MatchedGroupRepository = mock()
    private val groupRepository: GroupRepository = mock()
    private val groupMatchingAnswerFieldRepository: GroupMatchingAnswerFieldRepository = mock()
    private val groupMatchingFieldRepository: GroupMatchingFieldRepository = mock()
    private val groupMatchingSubjectRepository: GroupMatchingSubjectRepository = mock()
    private val groupMatchingRepository: GroupMatchingRepository = mock()
    private val groupMemberRepository: com.sight.repository.GroupMemberRepository = mock()
    private val groupMatchingRepository: com.sight.repository.GroupMatchingRepository = mock()
    private lateinit var groupMatchingService: GroupMatchingService

    @BeforeEach
    fun setUp() {
        groupMatchingService =
            GroupMatchingService(
                groupMatchingAnswerRepository = groupMatchingAnswerRepository,
                matchedGroupRepository = matchedGroupRepository,
                groupRepository = groupRepository,
                groupMatchingAnswerFieldRepository = groupMatchingAnswerFieldRepository,
                groupMatchingFieldRepository = groupMatchingFieldRepository,
                groupMatchingSubjectRepository = groupMatchingSubjectRepository,
                groupMemberRepository = groupMemberRepository,
                groupMatchingRepository = groupMatchingRepository,
            )
    }

    @Test
    fun `getGroups는 그룹과 멤버 정보를 반환한다`() {
        // given
        val groupMatchingId = "gm1"
        val groupType = GroupCategory.STUDY
        val answerId = "ans1"
        val groupId = 100L
        val memberId = 1L
        val createdAt = java.time.LocalDateTime.now()

        val answer =
            GroupMatchingAnswer(
                id = answerId,
                userId = memberId,
                groupType = groupType,
                isPreferOnline = false,
                groupMatchingId = groupMatchingId,
            )

        val matchedGroup =
            MatchedGroup(
                id = "mg1",
                groupId = groupId,
                answerId = answerId,
            )

        val projection = mock<GroupWithMemberProjection>()
        whenever(projection.groupId).thenReturn(groupId)
        whenever(projection.groupTitle).thenReturn("Test Group")
        whenever(projection.groupCreatedAt).thenReturn(createdAt)
        whenever(projection.memberId).thenReturn(memberId)
        whenever(projection.memberName).thenReturn("testuser")
        whenever(projection.memberRealName).thenReturn("Test User")
        whenever(projection.memberNumber).thenReturn(2020123456L)

        whenever(
            groupMatchingAnswerRepository.findAllByGroupMatchingIdAndGroupType(
                groupMatchingId,
                groupType,
            ),
        )
            .thenReturn(listOf(answer))
        whenever(matchedGroupRepository.findAllByAnswerIdIn(listOf(answerId)))
            .thenReturn(listOf(matchedGroup))
        whenever(groupRepository.findGroupsWithMembers(listOf(groupId)))
            .thenReturn(listOf(projection))

        // when
        val result = groupMatchingService.getGroups(groupMatchingId, groupType)

        // then
        assertEquals(1, result.size)
        assertEquals(groupId, result[0].id)
        assertEquals("Test Group", result[0].title)
        assertEquals(1, result[0].members.size)
        assertEquals(memberId, result[0].members[0].id)
        assertEquals(memberId, result[0].members[0].userId)
        assertEquals("Test User", result[0].members[0].name)
    }

    @Test
    fun `getAnswer는 답변이 없으면 NotFoundException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        val userId = 1L

        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        )
            .thenReturn(null)

        // when & then
        assertFailsWith<NotFoundException> {
            groupMatchingService.getAnswer(groupMatchingId, userId)
        }
    }

    @Test
    fun `addMemberToGroup은 그룹이 존재하지 않으면 예외를 던진다`() {
        // given
        val groupId = 100L
        val answerId = "ans1"

        whenever(groupRepository.findById(groupId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> { groupMatchingService.addMemberToGroup(groupId, answerId) }
    }

    @Test
    fun `addMemberToGroup은 답변이 존재하지 않으면 예외를 던진다`() {
        // given
        val groupId = 100L
        val answerId = "ans1"

        whenever(groupRepository.findById(groupId)).thenReturn(Optional.of(mock()))
        whenever(groupMatchingAnswerRepository.findById(answerId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> { groupMatchingService.addMemberToGroup(groupId, answerId) }
    }

    @Test
    fun `addMemberToGroup은 이미 그룹 멤버이면 예외를 던진다`() {
        // given
        val groupId = 100L
        val answerId = "ans1"
        val memberId = 1L
        val answer = mock<GroupMatchingAnswer>()
        whenever(answer.userId).thenReturn(memberId)

        whenever(groupRepository.findById(groupId)).thenReturn(Optional.of(mock()))
        whenever(groupMatchingAnswerRepository.findById(answerId)).thenReturn(Optional.of(answer))
        whenever(matchedGroupRepository.existsByGroupIdAndAnswerId(groupId, answerId))
            .thenReturn(false)
        whenever(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId))
            .thenReturn(true)

        // when & then
        assertThrows<BadRequestException> {
            groupMatchingService.addMemberToGroup(groupId, answerId)
        }
    }

    @Test
    fun `addMemberToGroup은 새로운 멤버를 추가하고 MatchedGroup을 생성한다`() {
        // given
        val groupId = 100L
        val answerId = "ans1"
        val memberId = 1L
        val answer = mock<GroupMatchingAnswer>()
        whenever(answer.userId).thenReturn(memberId)

        whenever(groupRepository.findById(groupId)).thenReturn(Optional.of(mock()))
        whenever(groupMatchingAnswerRepository.findById(answerId)).thenReturn(Optional.of(answer))
        whenever(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId))
            .thenReturn(false)
        whenever(matchedGroupRepository.existsByGroupIdAndAnswerId(groupId, answerId))
            .thenReturn(false)

        // when
        groupMatchingService.addMemberToGroup(groupId, answerId)

        // then
        verify(groupMemberRepository).save(groupId, memberId)
        verify(matchedGroupRepository).save(any<MatchedGroup>())
    }

    @Test
    fun `addMemberToGroup은 재가입 멤버를 추가하고 MatchedGroup은 생성하지 않는다`() {
        // given
        val groupId = 100L
        val answerId = "ans1"
        val memberId = 1L
        val answer = mock<GroupMatchingAnswer>()
        whenever(answer.userId).thenReturn(memberId)

        whenever(groupRepository.findById(groupId)).thenReturn(Optional.of(mock()))
        whenever(groupMatchingAnswerRepository.findById(answerId)).thenReturn(Optional.of(answer))
        whenever(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId))
            .thenReturn(false)
        whenever(matchedGroupRepository.existsByGroupIdAndAnswerId(groupId, answerId))
            .thenReturn(true)

        // when
        groupMatchingService.addMemberToGroup(groupId, answerId)

        // then
        verify(groupMemberRepository).save(groupId, memberId)
        verify(matchedGroupRepository, never()).save(any<MatchedGroup>())
    }

    @Test
    fun `updateClosedAt은 마감일을 성공적으로 업데이트한다`() {
        // given
        val groupMatchingId = "gm1"
        val currentClosedAt = java.time.LocalDateTime.now().plusDays(1)
        val newClosedAt = java.time.LocalDateTime.now().plusDays(7)

        val groupMatching =
            com.sight.domain.groupmatching.GroupMatching(
                id = groupMatchingId,
                year = 2024,
                semester = 2,
                closedAt = currentClosedAt,
            )

        val updatedGroupMatching =
            groupMatching.copy(
                closedAt = newClosedAt,
            )

        whenever(groupMatchingRepository.findById(groupMatchingId))
            .thenReturn(Optional.of(groupMatching))
        whenever(groupMatchingRepository.save(any<com.sight.domain.groupmatching.GroupMatching>()))
            .thenReturn(updatedGroupMatching)

        // when
        val result = groupMatchingService.updateClosedAt(groupMatchingId, newClosedAt)

        // then
        assertEquals(groupMatchingId, result.id)
        assertEquals(newClosedAt, result.closedAt)
        verify(groupMatchingRepository).save(any<com.sight.domain.groupmatching.GroupMatching>())
    }

    @Test
    fun `updateClosedAt은 존재하지 않는 그룹 매칭이면 NotFoundException을 던진다`() {
        // given
        val groupMatchingId = "nonexistent"
        val newClosedAt = java.time.LocalDateTime.now().plusDays(7)

        whenever(groupMatchingRepository.findById(groupMatchingId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            groupMatchingService.updateClosedAt(groupMatchingId, newClosedAt)
        }
    }

    @Test
    fun `updateClosedAt은 어제 이전 날짜의 마감일이면 BadRequestException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        // 그제 (어제 이전)
        val dayBeforeYesterday = java.time.LocalDateTime.now().minusDays(2)

        val groupMatching =
            com.sight.domain.groupmatching.GroupMatching(
                id = groupMatchingId,
                year = 2024,
                semester = 2,
                closedAt = java.time.LocalDateTime.now().plusDays(1),
            )

        whenever(groupMatchingRepository.findById(groupMatchingId))
            .thenReturn(Optional.of(groupMatching))

        // when & then
        assertThrows<BadRequestException> {
            groupMatchingService.updateClosedAt(groupMatchingId, dayBeforeYesterday)
        }
    }

    @Test
    fun `updateClosedAt은 어제 날짜의 마감일을 허용한다`() {
        // given
        val groupMatchingId = "gm1"
        // 어제
        val yesterday = java.time.LocalDateTime.now().minusDays(1)

        val groupMatching =
            com.sight.domain.groupmatching.GroupMatching(
                id = groupMatchingId,
                year = 2024,
                semester = 2,
                closedAt = java.time.LocalDateTime.now().plusDays(1),
            )

        val updatedGroupMatching =
            groupMatching.copy(
                closedAt = yesterday,
            )

        whenever(groupMatchingRepository.findById(groupMatchingId))
            .thenReturn(Optional.of(groupMatching))
        whenever(groupMatchingRepository.save(any<com.sight.domain.groupmatching.GroupMatching>()))
            .thenReturn(updatedGroupMatching)

        // when
        val result = groupMatchingService.updateClosedAt(groupMatchingId, yesterday)

        // then
        assertEquals(groupMatchingId, result.id)
        assertEquals(yesterday, result.closedAt)
        verify(groupMatchingRepository).save(any<com.sight.domain.groupmatching.GroupMatching>())
    }

    @Test
    fun `updateClosedAt은 오늘 날짜의 마감일을 허용한다`() {
        // given
        val groupMatchingId = "gm1"
        // 오늘
        val today = java.time.LocalDateTime.now()

        val groupMatching =
            com.sight.domain.groupmatching.GroupMatching(
                id = groupMatchingId,
                year = 2024,
                semester = 2,
                closedAt = java.time.LocalDateTime.now().plusDays(1),
            )

        val updatedGroupMatching =
            groupMatching.copy(
                closedAt = today,
            )

        whenever(groupMatchingRepository.findById(groupMatchingId))
            .thenReturn(Optional.of(groupMatching))
        whenever(groupMatchingRepository.save(any<com.sight.domain.groupmatching.GroupMatching>()))
            .thenReturn(updatedGroupMatching)

        // when
        val result = groupMatchingService.updateClosedAt(groupMatchingId, today)

        // then
        assertEquals(groupMatchingId, result.id)
        assertEquals(today, result.closedAt)
        verify(groupMatchingRepository).save(any<com.sight.domain.groupmatching.GroupMatching>())
    }
    
    fun `createGroupMatching은 중복이 없으면 성공적으로 그룹매칭을 생성한다`() {
        // Given
        val year = 2025
        val semester = 1
        val closedAt = LocalDateTime.now().plusDays(7)

        // 연도와 학기가 중복되지 않는다고 가정
        given(groupMatchingRepository.existsByYearAndSemester(year, semester))
            .willReturn(false)

        // save 호출 시 전달된 객체를 그대로 반환하도록 설정
        given(groupMatchingRepository.save(any<GroupMatching>())).willAnswer {
            it.arguments[0] as GroupMatching
        }

        // When
        val result = groupMatchingService.createGroupMatching(year, semester, closedAt)

        // Then
        assertEquals(year, result.year)
        assertEquals(semester, result.semester)
        assertEquals(closedAt, result.closedAt)

        // save가 1번 호출되었는지 검증
        verify(groupMatchingRepository).save(any<GroupMatching>())
    }

    // [시나리오 3: 연도와 학기가 중복인 그룹이 있을 때 -> 에러]
    @Test
    fun `createGroupMatching은 이미 존재하는 연도와 학기일 경우 UnprocessableEntityException을 던진다`() {
        // Given
        val year = 2025
        val semester = 1
        val closedAt = LocalDateTime.now().plusDays(7)

        // 이미 해당 연도와 학기가 존재한다고 가정
        given(groupMatchingRepository.existsByYearAndSemester(year, semester))
            .willReturn(true)

        // When & Then
        assertThrows<UnprocessableEntityException> {
            groupMatchingService.createGroupMatching(year, semester, closedAt)
        }

        // 예외가 발생했으므로 save는 호출되지 않아야 함
        verify(groupMatchingRepository, never()).save(any())
    }
}
