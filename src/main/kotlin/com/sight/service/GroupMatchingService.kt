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
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingAnswerOption
import com.sight.domain.groupmatching.GroupMatchingType
import com.sight.domain.groupmatching.MatchedGroup
import com.sight.repository.GroupMatchingAnswerOptionRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingOptionRepository
import com.sight.repository.GroupMatchingRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.service.dto.GroupMatchingAnswerDto
import com.sight.service.dto.GroupMatchingGroupDto
import com.sight.service.dto.GroupMatchingGroupMemberDto
import com.sight.service.dto.MatchedGroupResponse
import com.sight.service.dto.OptionResult
import com.sight.service.dto.UpdateGroupMatchingAnswerDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import kotlin.random.Random

@Service
class GroupMatchingService(
    private val groupMatchingAnswerRepository: GroupMatchingAnswerRepository,
    private val matchedGroupRepository: MatchedGroupRepository,
    private val groupRepository: GroupRepository,
    private val groupMatchingAnswerOptionRepository: GroupMatchingAnswerOptionRepository,
    private val groupMatchingOptionRepository: GroupMatchingOptionRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupMatchingRepository: GroupMatchingRepository,
) {
    @Transactional(readOnly = true)
    fun getGroups(
        groupMatchingId: String,
        groupType: GroupMatchingType?,
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
                ?: throw NotFoundException("답변을 찾을 수 없습니다")

        return buildAnswerDto(answer)
    }

    @Transactional
    fun updateAnswer(
        groupMatchingId: String,
        userId: Long,
        updateDto: UpdateGroupMatchingAnswerDto,
    ): GroupMatchingAnswerDto {
        val existingAnswer =
            groupMatchingAnswerRepository.findByGroupMatchingIdAndUserId(
                groupMatchingId,
                userId,
            )
                ?: throw NotFoundException("답변을 찾을 수 없습니다")

        val uniqueOptionIds = updateDto.selectedOptionIds.distinct()
        if (uniqueOptionIds.size != updateDto.selectedOptionIds.size) {
            throw BadRequestException("중복된 옵션이 포함되어 있습니다")
        }

        if (updateDto.selectedOptionIds.isNotEmpty()) {
            val existingOptions = groupMatchingOptionRepository.findAllById(updateDto.selectedOptionIds)
            val existingOptionIds = existingOptions.map { it.id }.toSet()
            val invalidOptionIds = updateDto.selectedOptionIds.filter { it !in existingOptionIds }

            if (invalidOptionIds.isNotEmpty()) {
                throw BadRequestException("존재하지 않는 옵션입니다: ${invalidOptionIds.joinToString(", ")}")
            }
        }

        val updatedAnswer =
            existingAnswer.copy(
                groupType = updateDto.groupType,
                isPreferOnline = updateDto.isPreferOnline,
                activityFrequency = updateDto.activityFrequency,
                activityFormat = updateDto.activityFormat,
                otherSuggestions = updateDto.otherSuggestions,
                customOption = updateDto.customOption,
                role = updateDto.role,
                hasIdea = updateDto.hasIdea,
                idea = updateDto.idea,
            )
        groupMatchingAnswerRepository.save(updatedAnswer)

        groupMatchingAnswerOptionRepository.deleteAllByAnswerId(existingAnswer.id)
        val answerOptions =
            updateDto.selectedOptionIds.map { optionId ->
                GroupMatchingAnswerOption(
                    id = UlidCreator.getUlid().toString(),
                    answerId = existingAnswer.id,
                    optionId = optionId,
                )
            }
        groupMatchingAnswerOptionRepository.saveAll(answerOptions)

        return buildAnswerDto(updatedAnswer)
    }

    private fun buildAnswerDto(answer: GroupMatchingAnswer): GroupMatchingAnswerDto {
        val answerOptions = groupMatchingAnswerOptionRepository.findAllByAnswerId(answer.id)
        val options =
            if (answerOptions.isNotEmpty()) {
                val optionIds = answerOptions.map { it.optionId }
                groupMatchingOptionRepository.findAllById(optionIds)
            } else {
                emptyList()
            }

        val matchedGroups = matchedGroupRepository.findAllByAnswerId(answer.id)

        return GroupMatchingAnswerDto(
            id = answer.id,
            userId = answer.userId,
            groupType = answer.groupType,
            isPreferOnline = answer.isPreferOnline,
            activityFrequency = answer.activityFrequency,
            activityFormat = answer.activityFormat,
            otherSuggestions = answer.otherSuggestions,
            groupMatchingId = answer.groupMatchingId,
            selectedOptions =
                options.map { option ->
                    OptionResult(
                        id = option.id,
                        name = option.name,
                    )
                },
            customOption = answer.customOption,
            role = answer.role,
            hasIdea = answer.hasIdea,
            idea = answer.idea,
            matchedGroups =
                matchedGroups.map { matchedGroup ->
                    MatchedGroupResponse(
                        id = matchedGroup.id,
                        groupId = matchedGroup.groupId,
                        createdAt = matchedGroup.createdAt,
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
        groupRepository.findById(groupId).orElseThrow {
            NotFoundException("그룹을 찾을 수 없습니다")
        }
        val answer =
            groupMatchingAnswerRepository.findById(answerId).orElseThrow {
                NotFoundException("답변을 찾을 수 없습니다")
            }

        if (groupMemberRepository.existsByGroupIdAndMemberId(groupId, answer.userId)) {
            throw BadRequestException("이미 그룹에 속한 멤버입니다")
        }

        groupMemberRepository.save(groupId, answer.userId)

        if (!matchedGroupRepository.existsByGroupIdAndAnswerId(groupId, answerId)) {
            matchedGroupRepository.save(
                MatchedGroup(
                    id = UlidCreator.getUlid().toString(),
                    groupId = groupId,
                    answerId = answerId,
                ),
            )
        }
    }

    @Transactional
    fun updateClosedAt(
        groupMatchingId: String,
        closedAt: LocalDate,
    ): GroupMatching {
        val groupMatching =
            groupMatchingRepository.findById(groupMatchingId).orElseThrow {
                NotFoundException("그룹 매칭을 찾을 수 없습니다")
            }

        val today = LocalDate.now(KST)
        val yesterday = today.minusDays(1)
        if (closedAt.isBefore(yesterday)) {
            throw BadRequestException("마감일은 어제 이전 날짜로 설정할 수 없습니다")
        }

        val closedAtInstant =
            closedAt
                .plusDays(1)
                .atStartOfDay(KST)
                .toInstant()

        val updatedGroupMatching =
            groupMatching.copy(
                closedAt = closedAtInstant,
            )

        return groupMatchingRepository.save(updatedGroupMatching)
    }

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

        val groupCategory =
            when (leaderAnswer.groupType) {
                GroupMatchingType.BASIC_LANGUAGE_STUDY, GroupMatchingType.PROJECT_STYLE_STUDY -> GroupCategory.STUDY
                GroupMatchingType.PRACTICAL_PROJECT -> GroupCategory.PROJECT
            }

        val newGroup =
            Group(
                id = createNewGroupId(),
                title = title,
                author = leaderUserId,
                master = leaderUserId,
                state = GroupState.PROGRESS,
                allowJoin = true,
                category = groupCategory,
                grade = GroupAccessGrade.MEMBER,
                countMember = answerIds.size.toLong(),
            )
        groupRepository.save(newGroup)

        val newGroupMembers = userIds.map { GroupMember(newGroup.id, it) }
        groupMemberRepository.saveAll(newGroupMembers)

        return newGroup.id
    }

    private fun createNewGroupId(): Long {
        val minimumId = 1000000

        val millisUntil20250101 =
            LocalDateTime.of(
                2025,
                Month.JANUARY,
                1,
                0,
                0,
                0,
            ).atZone(KST).toInstant().toEpochMilli()
        val currentTimestamp = System.currentTimeMillis()

        val timePart = (currentTimestamp - millisUntil20250101) / 1000 / 60 / 60

        val randomPart = Random(currentTimestamp).nextLong(0L, 1000L)

        return minimumId + timePart * 1000 + randomPart
    }

    @Transactional
    fun createGroupMatching(
        year: Int,
        semester: Int,
        closedAt: LocalDate,
        options: List<Pair<String, GroupMatchingType>>,
    ): GroupMatching {
        if (groupMatchingRepository.existsByYearAndSemester(year, semester)) {
            throw UnprocessableEntityException("해당 연도($year)와 학기($semester)의 그룹 매칭은 이미 존재합니다.")
        }

        val closedAtInstant =
            closedAt
                .plusDays(1)
                .atStartOfDay(KST)
                .toInstant()

        val groupMatching =
            GroupMatching(
                id = UlidCreator.getUlid().toString(),
                year = year,
                semester = semester,
                closedAt = closedAtInstant,
            )
        groupMatchingRepository.save(groupMatching)

        val optionEntities =
            options.map { (name, type) ->
                com.sight.domain.groupmatching.GroupMatchingOption(
                    id = UlidCreator.getUlid().toString(),
                    groupMatchingId = groupMatching.id,
                    name = name,
                    groupMatchingType = type,
                )
            }
        groupMatchingOptionRepository.saveAll(optionEntities)

        return groupMatching
    }

    @Transactional(readOnly = true)
    fun getOngoingGroupMatching(): GroupMatching {
        val now = Instant.now()
        val ongoingGroupMatchings = groupMatchingRepository.findAllByClosedAtAfter(now)

        return ongoingGroupMatchings
            .maxByOrNull { it.createdAt }
            ?: throw NotFoundException("진행 중인 그룹 매칭이 없습니다")
    }

    @Transactional(readOnly = true)
    fun listGroupMatchings(): List<GroupMatching> {
        return groupMatchingRepository.findAll()
            .sortedByDescending { it.createdAt }
    }

    companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
