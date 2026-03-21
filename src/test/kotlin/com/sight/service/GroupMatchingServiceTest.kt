package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.ActivityFrequency
import com.sight.domain.groupmatching.GroupMatching
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingType
import com.sight.domain.groupmatching.MatchedGroup
import com.sight.repository.GroupMatchingAnswerOptionRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingOptionRepository
import com.sight.repository.GroupMatchingRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.repository.projection.GroupWithMemberProjection
import com.sight.service.dto.UpdateGroupMatchingAnswerDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupMatchingServiceTest {
    private val groupMatchingAnswerRepository: GroupMatchingAnswerRepository = mock()
    private val matchedGroupRepository: MatchedGroupRepository = mock()
    private val groupRepository: GroupRepository = mock()
    private val groupMatchingAnswerOptionRepository: GroupMatchingAnswerOptionRepository = mock()
    private val groupMatchingOptionRepository: GroupMatchingOptionRepository = mock()
    private val groupMemberRepository: com.sight.repository.GroupMemberRepository = mock()
    private val groupMatchingRepository: GroupMatchingRepository = mock()
    private lateinit var groupMatchingService: GroupMatchingService

    @BeforeEach
    fun setUp() {
        groupMatchingService =
            GroupMatchingService(
                groupMatchingAnswerRepository = groupMatchingAnswerRepository,
                matchedGroupRepository = matchedGroupRepository,
                groupRepository = groupRepository,
                groupMatchingAnswerOptionRepository = groupMatchingAnswerOptionRepository,
                groupMatchingOptionRepository = groupMatchingOptionRepository,
                groupMemberRepository = groupMemberRepository,
                groupMatchingRepository = groupMatchingRepository,
            )
    }

    private fun createAnswer(
        id: String = "ans1",
        userId: Long = 1L,
        groupMatchingId: String = "gm1",
        groupType: GroupMatchingType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
    ): GroupMatchingAnswer {
        return GroupMatchingAnswer(
            id = id,
            userId = userId,
            groupType = groupType,
            isPreferOnline = false,
            activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
            activityFormat = "주 1회 오프라인",
            groupMatchingId = groupMatchingId,
        )
    }

    @Test
    fun `getGroups는 그룹과 멤버 정보를 반환한다`() {
        // given
        val groupMatchingId = "gm1"
        val groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY
        val answerId = "ans1"
        val groupId = 100L
        val memberId = 1L
        val createdAt = LocalDateTime.now()

        val answer =
            createAnswer(
                id = answerId,
                userId = memberId,
                groupMatchingId = groupMatchingId,
                groupType = groupType,
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
        ).thenReturn(listOf(answer))
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
        ).thenReturn(null)

        // when & then
        assertFailsWith<NotFoundException> {
            groupMatchingService.getAnswer(groupMatchingId, userId)
        }
    }

    @Test
    fun `updateAnswer는 답변이 없으면 NotFoundException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        val userId = 1L
        val updateDto =
            UpdateGroupMatchingAnswerDto(
                groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "주 1회 오프라인",
                otherSuggestions = null,
                selectedOptionIds = listOf("opt1"),
                customOption = null,
                role = null,
                hasIdea = null,
                idea = null,
            )

        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        ).thenReturn(null)

        // when & then
        assertFailsWith<NotFoundException> {
            groupMatchingService.updateAnswer(groupMatchingId, userId, updateDto)
        }
    }

    @Test
    fun `updateAnswer는 답변을 성공적으로 업데이트한다`() {
        // given
        val groupMatchingId = "gm1"
        val userId = 1L
        val answerId = "ans1"
        val existingAnswer =
            createAnswer(
                id = answerId,
                userId = userId,
                groupMatchingId = groupMatchingId,
                groupType = GroupMatchingType.PROJECT_STYLE_STUDY,
            )

        val updatedAnswer =
            existingAnswer.copy(
                groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.THREE_OR_FOUR,
                activityFormat = "주 3회 온라인",
            )

        val updateDto =
            UpdateGroupMatchingAnswerDto(
                groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.THREE_OR_FOUR,
                activityFormat = "주 3회 온라인",
                otherSuggestions = null,
                selectedOptionIds = listOf("opt1", "opt2"),
                customOption = null,
                role = null,
                hasIdea = null,
                idea = null,
            )

        val opt1 =
            com.sight.domain.groupmatching.GroupMatchingOption(
                id = "opt1",
                groupMatchingId = groupMatchingId,
                name = "Option 1",
                groupMatchingType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
            )
        val opt2 =
            com.sight.domain.groupmatching.GroupMatchingOption(
                id = "opt2",
                groupMatchingId = groupMatchingId,
                name = "Option 2",
                groupMatchingType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
            )

        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        )
            .thenReturn(existingAnswer)
            .thenReturn(updatedAnswer)
        whenever(groupMatchingAnswerOptionRepository.findAllByAnswerId(answerId))
            .thenReturn(emptyList())
        whenever(groupMatchingOptionRepository.findAllById(listOf("opt1", "opt2")))
            .thenReturn(listOf(opt1, opt2))
        whenever(matchedGroupRepository.findAllByAnswerId(answerId)).thenReturn(emptyList())

        // when
        val result = groupMatchingService.updateAnswer(groupMatchingId, userId, updateDto)

        // then
        verify(groupMatchingAnswerRepository).save(any<GroupMatchingAnswer>())
        verify(groupMatchingAnswerOptionRepository).deleteAllByAnswerId(answerId)
        assertEquals(answerId, result.id)
        assertEquals(GroupMatchingType.BASIC_LANGUAGE_STUDY, result.groupType)
        assertEquals(true, result.isPreferOnline)
    }

    @Test
    fun `updateAnswer는 중복된 optionIds가 있으면 BadRequestException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        val userId = 1L
        val existingAnswer = createAnswer(userId = userId, groupMatchingId = groupMatchingId)

        val updateDto =
            UpdateGroupMatchingAnswerDto(
                groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "주 1회 오프라인",
                otherSuggestions = null,
                selectedOptionIds = listOf("opt1", "opt1"),
                customOption = null,
                role = null,
                hasIdea = null,
                idea = null,
            )

        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        ).thenReturn(existingAnswer)

        // when & then
        assertThrows<BadRequestException> {
            groupMatchingService.updateAnswer(groupMatchingId, userId, updateDto)
        }
    }

    @Test
    fun `updateAnswer는 존재하지 않는 optionId가 있으면 BadRequestException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        val userId = 1L
        val existingAnswer = createAnswer(userId = userId, groupMatchingId = groupMatchingId)

        val updateDto =
            UpdateGroupMatchingAnswerDto(
                groupType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
                isPreferOnline = true,
                activityFrequency = ActivityFrequency.ONCE_OR_TWICE,
                activityFormat = "주 1회 오프라인",
                otherSuggestions = null,
                selectedOptionIds = listOf("opt1", "nonexistent"),
                customOption = null,
                role = null,
                hasIdea = null,
                idea = null,
            )

        val opt1 =
            com.sight.domain.groupmatching.GroupMatchingOption(
                id = "opt1",
                groupMatchingId = groupMatchingId,
                name = "Option 1",
                groupMatchingType = GroupMatchingType.BASIC_LANGUAGE_STUDY,
            )

        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        ).thenReturn(existingAnswer)
        whenever(groupMatchingOptionRepository.findAllById(listOf("opt1", "nonexistent")))
            .thenReturn(listOf(opt1))

        // when & then
        assertThrows<BadRequestException> {
            groupMatchingService.updateAnswer(groupMatchingId, userId, updateDto)
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
        val currentClosedAt = Instant.now().plusSeconds(86400)
        val newClosedAt = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7)

        val groupMatching =
            GroupMatching(
                id = groupMatchingId,
                year = 2024,
                semester = 2,
                closedAt = currentClosedAt,
            )

        whenever(groupMatchingRepository.findById(groupMatchingId))
            .thenReturn(Optional.of(groupMatching))
        whenever(groupMatchingRepository.save(any<GroupMatching>()))
            .thenAnswer { it.arguments[0] as GroupMatching }

        // when
        val result = groupMatchingService.updateClosedAt(groupMatchingId, newClosedAt)

        // then
        assertEquals(groupMatchingId, result.id)
        verify(groupMatchingRepository).save(any<GroupMatching>())
    }

    @Test
    fun `updateClosedAt은 존재하지 않는 그룹 매칭이면 NotFoundException을 던진다`() {
        // given
        val groupMatchingId = "nonexistent"
        val newClosedAt = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7)

        whenever(groupMatchingRepository.findById(groupMatchingId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            groupMatchingService.updateClosedAt(groupMatchingId, newClosedAt)
        }
    }

    @Test
    fun `createGroupFromGroupMatching은 모든 답변이 존재하고 리더가 멤버에 포함되면 그룹 ID를 반환한다`() {
        val title = "New Group"
        val answerIds = listOf("ans1", "ans2")
        val leaderUserId = 1L
        val answer1 = mock<GroupMatchingAnswer>()
        val answer2 = mock<GroupMatchingAnswer>()

        whenever(answer1.userId).thenReturn(1L)
        whenever(answer1.groupType).thenReturn(GroupMatchingType.BASIC_LANGUAGE_STUDY)
        whenever(answer2.userId).thenReturn(2L)
        whenever(groupMatchingAnswerRepository.findAllById(answerIds))
            .thenReturn(listOf(answer1, answer2))

        val result =
            groupMatchingService.createGroupFromGroupMatching(title, answerIds, leaderUserId)

        assert(result >= 1000000L)
        verify(groupRepository).save(any())
        verify(groupMemberRepository).saveAll(any())
    }

    @Test
    fun `createGroupFromGroupMatching은 답변이 일부 존재하지 않으면 예외를 던진다`() {
        val title = "New Group"
        val answerIds = listOf("ans1", "ans2")
        val leaderUserId = 1L
        val answer1 = mock<GroupMatchingAnswer>()

        whenever(groupMatchingAnswerRepository.findAllById(answerIds)).thenReturn(listOf(answer1))

        assertThrows<NotFoundException> {
            groupMatchingService.createGroupFromGroupMatching(title, answerIds, leaderUserId)
        }
    }

    @Test
    fun `updateClosedAt은 어제 이전 날짜의 마감일이면 BadRequestException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        val kst = ZoneId.of("Asia/Seoul")
        val dayBeforeYesterday = LocalDate.now(kst).minusDays(2)

        val groupMatching =
            GroupMatching(
                id = groupMatchingId,
                year = 2024,
                semester = 2,
                closedAt = Instant.now().plusSeconds(86400),
            )

        whenever(groupMatchingRepository.findById(groupMatchingId))
            .thenReturn(Optional.of(groupMatching))

        // when & then
        assertThrows<BadRequestException> {
            groupMatchingService.updateClosedAt(groupMatchingId, dayBeforeYesterday)
        }
    }

    @Test
    fun `createGroupFromGroupMatching은 리더가 멤버에 포함되지 않으면 예외를 던진다`() {
        val title = "New Group"
        val answerIds = listOf("ans1", "ans2")
        val leaderUserId = 3L
        val answer1 = mock<GroupMatchingAnswer>()
        val answer2 = mock<GroupMatchingAnswer>()

        whenever(answer1.userId).thenReturn(1L)
        whenever(answer2.userId).thenReturn(2L)
        whenever(groupMatchingAnswerRepository.findAllById(answerIds))
            .thenReturn(listOf(answer1, answer2))

        assertThrows<BadRequestException> {
            groupMatchingService.createGroupFromGroupMatching(title, answerIds, leaderUserId)
        }
    }

    @Test
    fun `updateClosedAt은 어제 날짜의 마감일을 허용한다`() {
        // given
        val groupMatchingId = "gm1"
        val kst = ZoneId.of("Asia/Seoul")
        val yesterday = LocalDate.now(kst).minusDays(1)

        val groupMatching =
            GroupMatching(
                id = groupMatchingId,
                year = 2024,
                semester = 2,
                closedAt = Instant.now().plusSeconds(86400),
            )

        whenever(groupMatchingRepository.findById(groupMatchingId))
            .thenReturn(Optional.of(groupMatching))
        whenever(groupMatchingRepository.save(any<GroupMatching>()))
            .thenAnswer { it.arguments[0] as GroupMatching }

        // when
        val result = groupMatchingService.updateClosedAt(groupMatchingId, yesterday)

        // then
        assertEquals(groupMatchingId, result.id)
        verify(groupMatchingRepository).save(any<GroupMatching>())
    }

    @Test
    fun `updateClosedAt은 오늘 날짜의 마감일을 허용한다`() {
        // given
        val groupMatchingId = "gm1"
        val kst = ZoneId.of("Asia/Seoul")
        val today = LocalDate.now(kst)

        val groupMatching =
            GroupMatching(
                id = groupMatchingId,
                year = 2024,
                semester = 2,
                closedAt = Instant.now().plusSeconds(86400),
            )

        whenever(groupMatchingRepository.findById(groupMatchingId))
            .thenReturn(Optional.of(groupMatching))
        whenever(groupMatchingRepository.save(any<GroupMatching>()))
            .thenAnswer { it.arguments[0] as GroupMatching }

        // when
        val result = groupMatchingService.updateClosedAt(groupMatchingId, today)

        // then
        assertEquals(groupMatchingId, result.id)
        verify(groupMatchingRepository).save(any<GroupMatching>())
    }

    @Test
    fun `createGroupMatching은 중복이 없으면 성공적으로 그룹매칭을 생성한다`() {
        // Given
        val year = 2025
        val semester = 1
        val closedAt = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7)

        given(groupMatchingRepository.existsByYearAndSemester(year, semester)).willReturn(false)

        given(groupMatchingRepository.save(any<GroupMatching>())).willAnswer {
            it.arguments[0] as GroupMatching
        }

        // When
        val result = groupMatchingService.createGroupMatching(year, semester, closedAt, emptyList())

        // Then
        assertEquals(year, result.year)
        assertEquals(semester, result.semester)

        verify(groupMatchingRepository).save(any<GroupMatching>())
    }

    @Test
    fun `createGroupMatching은 이미 존재하는 연도와 학기일 경우 UnprocessableEntityException을 던진다`() {
        // Given
        val year = 2025
        val semester = 1
        val closedAt = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7)

        given(groupMatchingRepository.existsByYearAndSemester(year, semester)).willReturn(true)

        // When & Then
        assertThrows<UnprocessableEntityException> {
            groupMatchingService.createGroupMatching(year, semester, closedAt, emptyList())
        }

        verify(groupMatchingRepository, never()).save(any())
    }

    @Test
    fun `getOngoingGroupMatching은 진행 중인 그룹 매칭 정보를 조회해야 한다`() {
        // Given
        val futureClosedAt = Instant.now().plusSeconds(86400 * 30)
        val groupMatching =
            GroupMatching(
                id = "test-id",
                year = 2025,
                semester = 2,
                closedAt = futureClosedAt,
            )
        whenever(groupMatchingRepository.findAllByClosedAtAfter(any()))
            .thenReturn(listOf(groupMatching))

        // When
        val result = groupMatchingService.getOngoingGroupMatching()

        // Then
        assertEquals(groupMatching.id, result.id)
        assertEquals(groupMatching.year, result.year)
        assertEquals(groupMatching.semester, result.semester)
        assertEquals(groupMatching.closedAt, result.closedAt)
        assertEquals(groupMatching.createdAt, result.createdAt)
    }

    @Test
    fun `getOngoingGroupMatching은 진행 중인 그룹 매칭 정보가 없다면 NotFoundException을 던진다`() {
        // Given
        whenever(groupMatchingRepository.findAllByClosedAtAfter(any())).thenReturn(emptyList())

        // When & Then
        assertFailsWith<NotFoundException> { groupMatchingService.getOngoingGroupMatching() }
    }

    @Test
    fun `getOngoingGroupMatching은 createdAt이 더 미래인 그룹 매칭을 반환한다`() {
        // Given
        val now = LocalDateTime.now()
        val olderGroupMatching =
            GroupMatching(
                id = "older-id",
                year = 2025,
                semester = 1,
                closedAt = Instant.now().plusSeconds(86400 * 60),
                createdAt = now.minusDays(10),
            )
        val newerGroupMatching =
            GroupMatching(
                id = "newer-id",
                year = 2025,
                semester = 2,
                closedAt = Instant.now().plusSeconds(86400 * 30),
                createdAt = now.minusDays(5),
            )

        whenever(groupMatchingRepository.findAllByClosedAtAfter(any()))
            .thenReturn(listOf(newerGroupMatching, olderGroupMatching))

        // When
        val result = groupMatchingService.getOngoingGroupMatching()

        // Then
        assertEquals("newer-id", result.id)
        assertEquals(2025, result.year)
        assertEquals(2, result.semester)
    }

    @Test
    fun `listGroupMatchings는 목록이 없으면 빈 리스트를 반환한다`() {
        // Given
        given(groupMatchingRepository.findAll()).willReturn(emptyList())

        // When
        val result = groupMatchingService.listGroupMatchings()

        // Then
        assertEquals(0, result.size)
        verify(groupMatchingRepository).findAll()
    }

    @Test
    fun `listGroupMatchings는 그룹 매칭 목록을 성공적으로 내림차순으로 조회한다`() {
        // Given
        val now = LocalDateTime.now()
        val oldestGroupMatching =
            GroupMatching(
                id = "gm1",
                year = 2024,
                semester = 1,
                closedAt = Instant.now().plusSeconds(86400 * 7),
                createdAt = now.minusDays(30),
            )
        val middleGroupMatching =
            GroupMatching(
                id = "gm2",
                year = 2024,
                semester = 2,
                closedAt = Instant.now().plusSeconds(86400 * 14),
                createdAt = now.minusDays(15),
            )
        val newestGroupMatching =
            GroupMatching(
                id = "gm3",
                year = 2025,
                semester = 1,
                closedAt = Instant.now().plusSeconds(86400 * 21),
                createdAt = now.minusDays(5),
            )

        given(groupMatchingRepository.findAll())
            .willReturn(listOf(oldestGroupMatching, middleGroupMatching, newestGroupMatching))

        // When
        val result = groupMatchingService.listGroupMatchings()

        // Then
        assertEquals(3, result.size)
        assertEquals("gm3", result[0].id)
        assertEquals("gm2", result[1].id)
        assertEquals("gm1", result[2].id)
        verify(groupMatchingRepository).findAll()
    }
}
