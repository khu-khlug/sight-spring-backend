package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupMember
import com.sight.domain.group.GroupState
import com.sight.domain.groupmatching.MatchedGroup
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.service.dto.GroupMatchingGroupDto
import com.sight.service.dto.GroupMatchingGroupMemberDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import kotlin.random.Random

@Service
class GroupMatchingService(
    private val groupMatchingAnswerRepository: GroupMatchingAnswerRepository,
    private val matchedGroupRepository: MatchedGroupRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
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

    @Transactional
    fun addMemberToGroup(
        groupId: Long,
        answerId: String,
    ) {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("Group not found") }
        val answer =
            groupMatchingAnswerRepository.findById(answerId)
                .orElseThrow { NotFoundException("Answer not found") }

        if (groupMemberRepository.existsByGroupIdAndMemberId(groupId, answer.userId)) {
            throw BadRequestException("Member already in group")
        }

        groupMemberRepository.save(groupId, answer.userId)

        // 이미 이전에 `MatchedGroup`이 생성되었으나 해당 회원이 그룹에서 나온 경우,
        // `MatchedGroup`은 존재하지만 `GroupMember`는 존재하지 않을 수 있음.
        if (!matchedGroupRepository.existsByGroupIdAndAnswerId(groupId, answerId)) {
            matchedGroupRepository.save(
                MatchedGroup(
                    id = java.util.UUID.randomUUID().toString(),
                    groupId = groupId,
                    answerId = answerId,
                ),
            )
        }
    }

    @Transactional
    fun createGroupFromGroupMatching(
        title: String,
        answerIds: List<String>,
        leaderUserId: Long,
    ): Long {
        val answers = groupMatchingAnswerRepository.findAllById(answerIds)
        if (answers.size != answerIds.size) {
            throw NotFoundException("Some answers not found")
        }

        val userIds = answers.map { it.userId }.toSet()
        if (leaderUserId !in userIds) {
            throw BadRequestException("Leader must be one of the group members")
        }

        val firstAnswer = answers.first()

        val newGroup =
            Group(
                id = createNewGroupId(),
                title = title,
                author = leaderUserId,
                master = leaderUserId,
                state = GroupState.PROGRESS,
                allowJoin = true,
                category = firstAnswer.groupType,
                grade = GroupAccessGrade.MEMBER,
                countMember = answerIds.size.toLong(),
            )
        groupRepository.save(newGroup)

        val newGroupMembers = userIds.map { GroupMember(newGroup.id, it) }
        groupMemberRepository.saveAll(newGroupMembers)

        // TODO: 그룹 로그 및 포인트 처리 등 후속 처리 추가 구현 예정

        return newGroup.id
    }

    // 레거시 방법으로 구현되어 있는 ID 생성 기법과 충돌되지 않도록 ID를 별도로 생성합니다.
    private fun createNewGroupId(): Long {
        val minimumId = 1000000 // 기존 ID와 충돌하지 않도록 최소 100만 이상의 값을 갖도록 합니다.

        val millisUntil20250101 =
            LocalDateTime.of(
                2025,
                Month.JANUARY,
                1,
                0,
                0,
                0,
            ).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
        val currentTimestamp = System.currentTimeMillis()

        // 시간 단위로 달라지도록 계산
        val timePart = (currentTimestamp - millisUntil20250101) / 1000 / 60 / 60

        // 시간 당 1/1000 확률로 충돌하도록 계산
        val randomPart = Random(currentTimestamp).nextLong(0L, 1000L)

        // 한 시간 당 1/1000 확률로 충돌되도록 ID 생성
        return minimumId + timePart * 1000 + randomPart
    }
}
