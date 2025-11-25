package com.sight.service

import com.sight.domain.group.GroupCategory
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.service.dto.GroupMatchingGroupDto
import com.sight.service.dto.GroupMatchingGroupMemberDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupMatchingService(
    private val groupMatchingAnswerRepository: GroupMatchingAnswerRepository,
    private val matchedGroupRepository: MatchedGroupRepository,
    private val groupRepository: GroupRepository,
) {
    @Transactional(readOnly = true)
    fun getGroups(
        groupMatchingId: String,
        groupType: GroupCategory?,
    ): List<GroupMatchingGroupDto> {
        val answers =
            if (groupType != null) {
                groupMatchingAnswerRepository.findAllByGroupMatchingIdAndGroupType(groupMatchingId, groupType)
            } else {
                groupMatchingAnswerRepository.findAllByGroupMatchingId(groupMatchingId)
            }

        if (answers.isEmpty()) {
            return emptyList()
        }

        val answerIds = answers.map { it.id }
        val matchedGroups = matchedGroupRepository.findAllByAnswerIdIn(answerIds)
        if (matchedGroups.isEmpty()) {
            return emptyList()
        }

        val groupIds = matchedGroups.map { it.groupId }.distinct()
        val projections = groupRepository.findGroupsWithMembers(groupIds)

        return projections.groupBy { it.groupId }
            .map { (groupId, members) ->
                val first = members.first()
                GroupMatchingGroupDto(
                    id = groupId,
                    title = first.groupTitle,
                    members =
                        members.map { member ->
                            GroupMatchingGroupMemberDto(
                                id = member.memberId,
                                userId = member.memberId,
                                name = member.memberRealName,
                                number = member.memberNumber,
                            )
                        },
                    createdAt = first.groupCreatedAt,
                )
            }
    }
}
