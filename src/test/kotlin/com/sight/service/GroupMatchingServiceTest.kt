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

        whenever(groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(groupMatchingId, userId))
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
        whenever(groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(groupMatchingId, userId))
            .thenReturn(existingAnswer)
            .thenReturn(updatedAnswer) // For getAnswer call at the end
        whenever(groupMatchingAnswerFieldRepository.findAllByAnswerId(answerId))
            .thenReturn(emptyList())
        whenever(groupMatchingFieldRepository.findAllById(listOf("field1", "field2")))
            .thenReturn(listOf(field1, field2))
        whenever(matchedGroupRepository.findAllByAnswerId(answerId))
            .thenReturn(emptyList())
        whenever(groupMatchingSubjectRepository.findAllByAnswerId(answerId))
            .thenReturn(emptyList())

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

        whenever(groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(groupMatchingId, userId))
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

        whenever(groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(groupMatchingId, userId))
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

        whenever(groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(groupMatchingId, userId))
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
}
