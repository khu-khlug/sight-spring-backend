package com.sight.service

import com.sight.domain.group.GroupCategory
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.MatchedGroup
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MatchedGroupRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class GroupMatchingServiceTest {
    private val groupMatchingAnswerRepository: GroupMatchingAnswerRepository = mock()
    private val matchedGroupRepository: MatchedGroupRepository = mock()
    private val groupRepository: GroupRepository = mock()
    private lateinit var groupMatchingService: GroupMatchingService

    @BeforeEach
    fun setUp() {
        groupMatchingService =
            GroupMatchingService(
                groupMatchingAnswerRepository = groupMatchingAnswerRepository,
                matchedGroupRepository = matchedGroupRepository,
                groupRepository = groupRepository,
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

        val projection = mock<com.sight.repository.projection.GroupWithMemberProjection>()
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
}
