package com.sight.service

import com.sight.domain.group.GroupCategory
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.core.exception.NotFoundException
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.controllers.http.dto.GetGroupMatchingAnswerResponse
import com.sight.service.dto.GroupMatchingGroupDto
import com.sight.service.dto.GroupMatchingGroupMemberDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupMatchingService(
    private val groupMatchingAnswerRepository: GroupMatchingAnswerRepository,
    private val matchedGroupRepository: MatchedGroupRepository,
    private val groupRepository: GroupRepository,
    private val groupMatchingAnswerFieldRepository: GroupMatchingAnswerFieldRepository,
    private val groupMatchingFieldRepository: GroupMatchingFieldRepository,
    private val groupMatchingSubjectRepository: GroupMatchingSubjectRepository,
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

    @Transactional(readOnly = true)
    fun getAnswer(
        groupMatchingId: String,
        userId: Long,
    ): GetGroupMatchingAnswerResponse {
        val answer =
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(groupMatchingId, userId)
                ?: throw NotFoundException("Answer not found")

        val answerFields = groupMatchingAnswerFieldRepository.findAllByAnswerId(answer.id)
        val fields =
            if (answerFields.isNotEmpty()) {
                val fieldIds = answerFields.map { it.fieldId }
                groupMatchingFieldRepository.findAllById(fieldIds)
            } else {
                emptyList()
            }

        val matchedGroups = matchedGroupRepository.findAllByAnswerId(answer.id)
        val subjects = groupMatchingSubjectRepository.findAllByAnswerId(answer.id)

        return GetGroupMatchingAnswerResponse(
            id = answer.id,
            userId = answer.userId,
            groupType = answer.groupType,
            isPreferOnline = answer.isPreferOnline,
            groupMatchingId = answer.groupMatchingId,
            fields =
                fields.map { field ->
                    GetGroupMatchingAnswerResponse.FieldResponse(
                        id = field.id,
                        name = field.name,
                    )
                },
            matchedGroups =
                matchedGroups.map { matchedGroup ->
                    GetGroupMatchingAnswerResponse.MatchedGroupResponse(
                        id = matchedGroup.id,
                        groupId = matchedGroup.groupId,
                        createdAt = matchedGroup.createdAt,
                    )
                },
            groupMatchingSubjects =
                subjects.map { subject ->
                    GetGroupMatchingAnswerResponse.GroupMatchingSubjectResponse(
                        id = subject.id,
                        subject = subject.subject,
                    )
                },
            createdAt = answer.createdAt,
            updatedAt = answer.updatedAt,
        )
    }
}
