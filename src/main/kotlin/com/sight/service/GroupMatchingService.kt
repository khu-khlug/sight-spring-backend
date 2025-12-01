package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupAccessGrade
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupMember
import com.sight.domain.group.GroupState
import com.sight.domain.groupmatching.GroupMatching
import com.sight.domain.groupmatching.MatchedGroup
import com.sight.repository.GroupMatchingAnswerFieldRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingRepository
import com.sight.repository.GroupMatchingSubjectRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.service.dto.FieldResponse
import com.sight.service.dto.GroupMatchingAnswerDto
import com.sight.service.dto.GroupMatchingGroupDto
import com.sight.service.dto.GroupMatchingGroupMemberDto
import com.sight.service.dto.GroupMatchingSubjectResponse
import com.sight.service.dto.MatchedGroupResponse
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
    private val groupMatchingAnswerFieldRepository: GroupMatchingAnswerFieldRepository,
    private val groupMatchingFieldRepository: GroupMatchingFieldRepository,
    private val groupMatchingRepository: GroupMatchingRepository,
    private val groupMatchingSubjectRepository: GroupMatchingSubjectRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {
    @Transactional(readOnly = true)
    fun getGroups(
        groupMatchingId: String,
        groupType: GroupCategory?,
    ): List<GroupMatchingGroupDto> {
        val answers =
            if (groupType != null) {
                groupMatchingAnswerRepository.findAllByGroupMatchingIdAndGroupType(
                    groupMatchingId,
                    groupType,
                )
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

        return projections.groupBy { it.groupId }.map { (groupId, members) ->
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
    ): GroupMatchingAnswerDto {
        val answer =
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            )
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

        return GroupMatchingAnswerDto(
            id = answer.id,
            userId = answer.userId,
            groupType = answer.groupType,
            isPreferOnline = answer.isPreferOnline,
            groupMatchingId = answer.groupMatchingId,
            fields =
                fields.map { field ->
                    FieldResponse(
                        id = field.id,
                        name = field.name,
                    )
                },
            matchedGroups =
                matchedGroups.map { matchedGroup ->
                    MatchedGroupResponse(
                        id = matchedGroup.id,
                        groupId = matchedGroup.groupId,
                        createdAt = matchedGroup.createdAt,
                    )
                },
            groupMatchingSubjects =
                subjects.map { subject ->
                    GroupMatchingSubjectResponse(
                        id = subject.id,
                        subject = subject.subject,
                    )
                },
            createdAt = answer.createdAt,
            updatedAt = answer.updatedAt,
        )
    }

    @Transactional
    fun addMemberToGroup(
        groupId: Long,
        answerId: String,
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("Group not found")
            }
        val answer =
            groupMatchingAnswerRepository.findById(answerId).orElseThrow {
                NotFoundException("Answer not found")
            }

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
            throw NotFoundException("주어진 그룹 매칭 응답 중 존재하지 않는 것이 있습니다")
        }

        val userIds = answers.map { it.userId }.toSet()
        val leaderAnswer = answers.find { it.userId == leaderUserId }

        if (leaderAnswer == null) {
            throw BadRequestException("그룹장은 주어진 그룹 매칭의 응답 제출자들 중 한 명이어야 합니다")
        }

        val newGroup =
            Group(
                id = createNewGroupId(),
                title = title,
                author = leaderUserId,
                master = leaderUserId,
                state = GroupState.PROGRESS,
                allowJoin = true,
                category = leaderAnswer.groupType,
                grade = GroupAccessGrade.MEMBER,
                countMember = answerIds.size.toLong(),
            )
        groupRepository.save(newGroup)

        val newGroupMembers = userIds.map { GroupMember(newGroup.id, it) }
        groupMemberRepository.saveAll(newGroupMembers)

        // TODO: 그룹 로그 추가 등 후속 처리 구현 예정

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

    @Transactional
    fun createGroupMatching(
        year: Int,
        semester: Int,
        closedAt: LocalDateTime,
    ): GroupMatching {
        if (groupMatchingRepository.existsByYearAndSemester(year, semester)) {
            throw UnprocessableEntityException("해당 연도($year)와 학기($semester)의 그룹 매칭은 이미 존재합니다.")
        }

        val groupMatching =
            GroupMatching(
                id = UlidCreator.getUlid().toString(),
                year = year,
                semester = semester,
                closedAt = closedAt,
            )

        return groupMatchingRepository.save(groupMatching)
    }
}
