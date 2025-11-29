package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.MatchedGroup
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.repository.projection.GroupWithMemberProjection
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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

        whenever(groupMatchingAnswerRepository.findAllByGroupMatchingIdAndGroupType(groupMatchingId, groupType))
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

        whenever(groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(groupMatchingId, userId))
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
        assertThrows<NotFoundException> {
            groupMatchingService.addMemberToGroup(groupId, answerId)
        }
    }

    @Test
    fun `addMemberToGroup은 답변이 존재하지 않으면 예외를 던진다`() {
        // given
        val groupId = 100L
        val answerId = "ans1"

        whenever(groupRepository.findById(groupId)).thenReturn(Optional.of(mock()))
        whenever(groupMatchingAnswerRepository.findById(answerId)).thenReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            groupMatchingService.addMemberToGroup(groupId, answerId)
        }
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
        whenever(matchedGroupRepository.existsByGroupIdAndAnswerId(groupId, answerId)).thenReturn(false)
        whenever(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).thenReturn(true)

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
        whenever(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).thenReturn(false)
        whenever(matchedGroupRepository.existsByGroupIdAndAnswerId(groupId, answerId)).thenReturn(false)

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
        whenever(groupMemberRepository.existsByGroupIdAndMemberId(groupId, memberId)).thenReturn(false)
        whenever(matchedGroupRepository.existsByGroupIdAndAnswerId(groupId, answerId)).thenReturn(true)

        // when
        groupMatchingService.addMemberToGroup(groupId, answerId)

        // then
        verify(groupMemberRepository).save(groupId, memberId)
        verify(matchedGroupRepository, never()).save(any<MatchedGroup>())
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
        whenever(groupMatchingAnswerRepository.findAllById(answerIds)).thenReturn(listOf(answer1, answer2))

        val result = groupMatchingService.createGroupFromGroupMatching(title, answerIds, leaderUserId)

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
    fun `createGroupFromGroupMatching은 리더가 멤버에 포함되지 않으면 예외를 던진다`() {
        val title = "New Group"
        val answerIds = listOf("ans1", "ans2")
        val leaderUserId = 3L
        val answer1 = mock<GroupMatchingAnswer>()
        val answer2 = mock<GroupMatchingAnswer>()

        whenever(answer1.userId).thenReturn(1L)
        whenever(answer2.userId).thenReturn(2L)
        whenever(groupMatchingAnswerRepository.findAllById(answerIds)).thenReturn(listOf(answer1, answer2))

        assertThrows<BadRequestException> {
            groupMatchingService.createGroupFromGroupMatching(title, answerIds, leaderUserId)
        }
    }
}
