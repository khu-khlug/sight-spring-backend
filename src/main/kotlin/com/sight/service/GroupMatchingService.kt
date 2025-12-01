package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupCategory
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
    private val groupMatchingRepository: com.sight.repository.GroupMatchingRepository,
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
    fun updateClosedAt(
        groupMatchingId: String,
        closedAt: java.time.LocalDateTime,
    ): com.sight.domain.groupmatching.GroupMatching {
        // 1. 그룹 매칭 조회
        val groupMatching =
            groupMatchingRepository.findById(groupMatchingId).orElseThrow {
                NotFoundException("Group matching not found")
            }

        // 2. closedAt 유효성 검증 (어제 이전 날짜인지 확인)
        val kst = java.time.ZoneId.of("Asia/Seoul")
        val now = java.time.ZonedDateTime.now(kst)
        val today = now.toLocalDate()
        val closedAtDate = closedAt.toLocalDate()
        val yesterday = today.minusDays(1)
        if (closedAtDate.isBefore(yesterday)) {
            throw BadRequestException("마감일은 어제 이전 날짜로 설정할 수 없습니다")
        }

        // 3. closedAt 업데이트
        val updatedGroupMatching =
            groupMatching.copy(
                closedAt = closedAt,
            )

        return groupMatchingRepository.save(updatedGroupMatching)
    }

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
