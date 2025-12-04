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
    fun `updateAnswer는 답변이 없으면 NotFoundException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        val userId = 1L
        val updateDto =
            com.sight.service.dto.UpdateGroupMatchingAnswerDto(
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                fieldIds = listOf("field1"),
                subjects = listOf("subject1"),
            )

        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        )
            .thenReturn(null)

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
            GroupMatchingAnswer(
                id = answerId,
                userId = userId,
                groupType = GroupCategory.PROJECT,
                isPreferOnline = false,
                groupMatchingId = groupMatchingId,
            )

        val updatedAnswer =
            GroupMatchingAnswer(
                id = answerId,
                userId = userId,
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                groupMatchingId = groupMatchingId,
            )

        val updateDto =
            com.sight.service.dto.UpdateGroupMatchingAnswerDto(
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                fieldIds = listOf("field1", "field2"),
                subjects = listOf("subject1"),
            )

        val field1 =
            com.sight.domain.groupmatching.GroupMatchingField(
                id = "field1",
                name = "Field 1",
            )
        val field2 =
            com.sight.domain.groupmatching.GroupMatchingField(
                id = "field2",
                name = "Field 2",
            )

        // Mock for initial findByGroupMatchingIdAndUserId (in updateAnswer)
        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        )
            .thenReturn(existingAnswer)
            .thenReturn(updatedAnswer) // For getAnswer call at the end
        whenever(groupMatchingAnswerFieldRepository.findAllByAnswerId(answerId))
            .thenReturn(emptyList())
        whenever(groupMatchingFieldRepository.findAllById(listOf("field1", "field2")))
            .thenReturn(listOf(field1, field2))
        whenever(matchedGroupRepository.findAllByAnswerId(answerId)).thenReturn(emptyList())
        whenever(groupMatchingSubjectRepository.findAllByAnswerId(answerId)).thenReturn(emptyList())

        // when
        val result = groupMatchingService.updateAnswer(groupMatchingId, userId, updateDto)

        // then
        verify(groupMatchingAnswerRepository).save(any<GroupMatchingAnswer>())
        verify(groupMatchingAnswerFieldRepository).deleteAllByAnswerId(answerId)
        verify(groupMatchingSubjectRepository).deleteAllByAnswerId(answerId)
        assertEquals(answerId, result.id)
        assertEquals(GroupCategory.STUDY, result.groupType)
        assertEquals(true, result.isPreferOnline)
    }

    @Test
    fun `updateAnswer는 중복된 fieldIds가 있으면 BadRequestException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        val userId = 1L
        val answerId = "ans1"
        val existingAnswer =
            GroupMatchingAnswer(
                id = answerId,
                userId = userId,
                groupType = GroupCategory.PROJECT,
                isPreferOnline = false,
                groupMatchingId = groupMatchingId,
            )

        val updateDto =
            com.sight.service.dto.UpdateGroupMatchingAnswerDto(
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                // 중복된 fieldId
                fieldIds = listOf("field1", "field1"),
                subjects = emptyList(),
            )

        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        )
            .thenReturn(existingAnswer)

        // when & then
        assertThrows<BadRequestException> {
            groupMatchingService.updateAnswer(groupMatchingId, userId, updateDto)
        }
    }

    @Test
    fun `updateAnswer는 존재하지 않는 fieldId가 있으면 BadRequestException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        val userId = 1L
        val answerId = "ans1"
        val existingAnswer =
            GroupMatchingAnswer(
                id = answerId,
                userId = userId,
                groupType = GroupCategory.PROJECT,
                isPreferOnline = false,
                groupMatchingId = groupMatchingId,
            )

        val updateDto =
            com.sight.service.dto.UpdateGroupMatchingAnswerDto(
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                fieldIds = listOf("field1", "nonexistent"),
                subjects = emptyList(),
            )

        val field1 =
            com.sight.domain.groupmatching.GroupMatchingField(
                id = "field1",
                name = "Field 1",
            )

        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        )
            .thenReturn(existingAnswer)
        whenever(groupMatchingFieldRepository.findAllById(listOf("field1", "nonexistent")))
            .thenReturn(listOf(field1)) // field1만 존재, nonexistent는 없음

        // when & then
        assertThrows<BadRequestException> {
            groupMatchingService.updateAnswer(groupMatchingId, userId, updateDto)
        }
    }

    @Test
    fun `updateAnswer는 공백 subject가 있으면 BadRequestException을 던진다`() {
        // given
        val groupMatchingId = "gm1"
        val userId = 1L
        val answerId = "ans1"
        val existingAnswer =
            GroupMatchingAnswer(
                id = answerId,
                userId = userId,
                groupType = GroupCategory.PROJECT,
                isPreferOnline = false,
                groupMatchingId = groupMatchingId,
            )

        val updateDto =
            com.sight.service.dto.UpdateGroupMatchingAnswerDto(
                groupType = GroupCategory.STUDY,
                isPreferOnline = true,
                fieldIds = listOf("field1"),
                // 공백 포함
                subjects = listOf("subject1", "  "),
            )

        val field1 =
            com.sight.domain.groupmatching.GroupMatchingField(
                id = "field1",
                name = "Field 1",
            )

        whenever(
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            ),
        )
            .thenReturn(existingAnswer)
        whenever(groupMatchingFieldRepository.findAllById(listOf("field1")))
            .thenReturn(listOf(field1))

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
    fun `createGroupFromGroupMatching은 모든 답변이 존재하고 리더가 멤버에 포함되면 그룹 ID를 반환한다`() {
        val title = "New Group"
        val answerIds = listOf("ans1", "ans2")
        val leaderUserId = 1L
        val answer1 = mock<GroupMatchingAnswer>()
        val answer2 = mock<GroupMatchingAnswer>()

        whenever(answer1.userId).thenReturn(1L)
        whenever(answer1.groupType).thenReturn(GroupCategory.STUDY)
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
        // 그제 (어제 이전)
        val kst = java.time.ZoneId.of("Asia/Seoul")
        val dayBeforeYesterday = java.time.ZonedDateTime.now(kst).toLocalDateTime().minusDays(2)

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
        // 어제
        val kst = java.time.ZoneId.of("Asia/Seoul")
        val yesterday = java.time.ZonedDateTime.now(kst).toLocalDateTime().minusDays(1)

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
        val kst = java.time.ZoneId.of("Asia/Seoul")
        val today = java.time.ZonedDateTime.now(kst).toLocalDateTime()

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

    @Test
    fun `createGroupMatching은 중복이 없으면 성공적으로 그룹매칭을 생성한다`() {
        // Given
        val year = 2025
        val semester = 1
        val closedAt = LocalDateTime.now().plusDays(7)

        // 연도와 학기가 중복되지 않는다고 가정
        given(groupMatchingRepository.existsByYearAndSemester(year, semester)).willReturn(false)

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
        given(groupMatchingRepository.existsByYearAndSemester(year, semester)).willReturn(true)

        // When & Then
        assertThrows<UnprocessableEntityException> {
            groupMatchingService.createGroupMatching(year, semester, closedAt)
        }

        // 예외가 발생했으므로 save는 호출되지 않아야 함
        verify(groupMatchingRepository, never()).save(any())
    }

    @Test
    fun `getOngoingGroupMatching은 진행 중인 그룹 매칭 정보를 조회해야 한다`() {
        // Given: closedAt이 현재 시점보다 1달 뒤인 그룹 매칭 정보가 존재한다
        val now = LocalDateTime.now()
        val futureClosedAt = now.plusMonths(1)
        val groupMatching =
            GroupMatching(
                id = "test-id",
                year = 2025,
                semester = 2,
                closedAt = futureClosedAt,
                createdAt = now,
            )
        whenever(groupMatchingRepository.findAllByClosedAtAfter(any()))
            .thenReturn(listOf(groupMatching))

        // When: API를 호출한다
        val result = groupMatchingService.getOngoingGroupMatching()

        // Then: 해당 그룹 매칭 정보를 반환한다
        assertEquals(groupMatching.id, result.id)
        assertEquals(groupMatching.year, result.year)
        assertEquals(groupMatching.semester, result.semester)
        assertEquals(groupMatching.closedAt, result.closedAt)
        assertEquals(groupMatching.createdAt, result.createdAt)
    }

    @Test
    fun `getOngoingGroupMatching은 진행 중인 그룹 매칭 정보가 없다면 NotFoundException을 던진다`() {
        // Given: closedAt이 현재 시점보다 이후인 그룹 매칭 정보가 존재하지 않는다
        whenever(groupMatchingRepository.findAllByClosedAtAfter(any())).thenReturn(emptyList())

        // When & Then: API를 호출하면 404 에러가 발생한다
        assertFailsWith<NotFoundException> { groupMatchingService.getOngoingGroupMatching() }
    }

    @Test
    fun `getOngoingGroupMatching은 createdAt이 더 미래인 그룹 매칭을 반환한다`() {
        // Given: closedAt이 현재 시점보다 이후인 그룹 매칭 정보가 2개이다
        val now = LocalDateTime.now()
        val olderGroupMatching =
            GroupMatching(
                id = "older-id",
                year = 2025,
                semester = 1,
                closedAt = now.plusMonths(2),
                createdAt = now.minusDays(10),
            )
        val newerGroupMatching =
            GroupMatching(
                id = "newer-id",
                year = 2025,
                semester = 2,
                closedAt = now.plusMonths(1),
                createdAt = now.minusDays(5),
            )

        whenever(groupMatchingRepository.findAllByClosedAtAfter(any()))
            .thenReturn(listOf(newerGroupMatching, olderGroupMatching))

        // When: API를 호출한다
        val result = groupMatchingService.getOngoingGroupMatching()

        // Then: createdAt이 더 미래인 것을 반환한다
        assertEquals("newer-id", result.id)
        assertEquals(2025, result.year)
        assertEquals(2, result.semester)
    }
}
