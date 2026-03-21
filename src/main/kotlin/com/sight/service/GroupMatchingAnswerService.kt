package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.ActivityFrequency
import com.sight.domain.groupmatching.GroupMatchingAnswer
import com.sight.domain.groupmatching.GroupMatchingAnswerOption
import com.sight.domain.groupmatching.GroupMatchingType
import com.sight.repository.GroupMatchingAnswerOptionRepository
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingOptionRepository
import com.sight.repository.GroupMatchingRepository
import com.sight.repository.MatchedGroupRepository
import com.sight.repository.MemberRepository
import com.sight.service.GroupMatchingService.Companion.KST
import com.sight.service.dto.AnswerSummary
import com.sight.service.dto.GroupMatchingAnswerResult
import com.sight.service.dto.ListAnswersResult
import com.sight.service.dto.OptionResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class GroupMatchingAnswerService(
    private val answerRepository: GroupMatchingAnswerRepository,
    private val answerOptionRepository: GroupMatchingAnswerOptionRepository,
    private val matchedGroupRepository: MatchedGroupRepository,
    private val optionRepository: GroupMatchingOptionRepository,
    private val groupMatchingRepository: GroupMatchingRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun createGroupMatchingAnswer(
        groupType: GroupMatchingType,
        isPreferOnline: Boolean,
        activityFrequency: ActivityFrequency,
        activityFormat: String,
        otherSuggestions: String?,
        userId: Long,
        groupMatchingId: String,
        selectedOptionIds: List<String>,
        customOption: String?,
        role: String?,
        hasIdea: Boolean?,
        idea: String?,
    ): GroupMatchingAnswerResult {
        val groupMatching =
            groupMatchingRepository.findById(groupMatchingId)
                .orElseThrow { NotFoundException("해당 그룹 매칭을 찾을 수 없습니다.") }

        val now = Instant.now()

        if (!now.isBefore(groupMatching.closedAt)) {
            val deadlineKst =
                groupMatching.closedAt
                    .atZone(KST)
                    .minusSeconds(1)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            throw UnprocessableEntityException("그룹 매칭 응답 기간이 마감되었습니다. (마감일시: $deadlineKst KST)")
        }

        if (answerRepository.existsByUserIdAndGroupMatchingId(userId, groupMatchingId)) {
            throw UnprocessableEntityException("이미 응답을 제출했습니다.")
        }

        validateOptionIds(selectedOptionIds, groupMatchingId, groupType)

        val answer =
            GroupMatchingAnswer(
                id = UlidCreator.getUlid().toString(),
                userId = userId,
                groupType = groupType,
                isPreferOnline = isPreferOnline,
                activityFrequency = activityFrequency,
                activityFormat = activityFormat,
                otherSuggestions = otherSuggestions,
                customOption = customOption,
                role = role,
                hasIdea = hasIdea,
                idea = idea,
                groupMatchingId = groupMatchingId,
            )
        val savedAnswer = answerRepository.save(answer)

        val optionEntities =
            selectedOptionIds.map { optionId ->
                GroupMatchingAnswerOption(
                    id = UlidCreator.getUlid().toString(),
                    answerId = savedAnswer.id,
                    optionId = optionId,
                )
            }
        answerOptionRepository.saveAll(optionEntities)

        val savedOptions =
            optionRepository.findAllById(selectedOptionIds)
                .map { OptionResult(id = it.id, name = it.name) }

        return GroupMatchingAnswerResult(
            answer = savedAnswer,
            options = savedOptions,
        )
    }

    @Transactional(readOnly = true)
    fun listAnswers(
        groupMatchingId: String,
        groupType: GroupMatchingType? = null,
        optionId: String? = null,
        offset: Int,
        limit: Int,
    ): ListAnswersResult {
        if (optionId != null) {
            optionRepository.findById(optionId)
                .orElseThrow { BadRequestException("유효하지 않은 옵션입니다") }
        }

        val pageNumber = offset / limit
        val pageable = PageRequest.of(pageNumber, limit)

        val page = answerRepository.findAnswersWithFilters(groupMatchingId, groupType, optionId, pageable)

        val userIds = page.content.map { it.userId }.distinct()
        val membersById = memberRepository.findAllById(userIds).associateBy { it.id }

        val answerSummaries =
            page.content.map { answer ->
                val selectedOptions = getSelectedOptions(answer.id)
                val matchedGroupIds = getMatchedGroupIds(answer.id)
                val member = membersById[answer.userId]

                AnswerSummary(
                    answerId = answer.id,
                    answerUserId = answer.userId,
                    answerUserName = member?.realname ?: "",
                    answerUserNumber = member?.number,
                    createdAt = answer.createdAt,
                    updatedAt = answer.updatedAt,
                    groupType = answer.groupType,
                    isPreferOnline = answer.isPreferOnline,
                    activityFrequency = answer.activityFrequency,
                    activityFormat = answer.activityFormat,
                    otherSuggestions = answer.otherSuggestions,
                    selectedOptions = selectedOptions,
                    customOption = answer.customOption,
                    role = answer.role,
                    hasIdea = answer.hasIdea,
                    idea = answer.idea,
                    matchedGroupIds = matchedGroupIds,
                )
            }

        return ListAnswersResult(
            answers = answerSummaries,
            count = page.totalElements.toInt(),
        )
    }

    private fun getSelectedOptions(answerId: String): List<OptionResult> {
        val answerOptions = answerOptionRepository.findAllByAnswerId(answerId)
        if (answerOptions.isEmpty()) {
            return emptyList()
        }
        val optionIds = answerOptions.map { it.optionId }
        val options = optionRepository.findAllById(optionIds)
        return options.map { OptionResult(id = it.id, name = it.name) }
    }

    private fun getMatchedGroupIds(answerId: String): List<Long> {
        return matchedGroupRepository.findAllByAnswerId(answerId)
            .map { it.groupId }
    }

    private fun validateOptionIds(
        selectedOptionIds: List<String>,
        groupMatchingId: String,
        groupType: GroupMatchingType,
    ) {
        if (selectedOptionIds.isEmpty()) return

        val uniqueOptionIds = selectedOptionIds.distinct()
        if (uniqueOptionIds.size != selectedOptionIds.size) {
            throw BadRequestException("중복된 옵션이 포함되어 있습니다")
        }

        val existingOptions = optionRepository.findAllById(selectedOptionIds)
        val existingOptionIds = existingOptions.map { it.id }.toSet()
        val invalidOptionIds = selectedOptionIds.filter { it !in existingOptionIds }

        if (invalidOptionIds.isNotEmpty()) {
            throw BadRequestException("존재하지 않는 옵션입니다: ${invalidOptionIds.joinToString(", ")}")
        }

        existingOptions.forEach { option ->
            if (option.groupMatchingId != groupMatchingId) {
                throw BadRequestException("해당 그룹 매칭에 속하지 않는 옵션입니다: ${option.id}")
            }
            if (option.groupMatchingType != groupType) {
                throw BadRequestException("선택한 그룹 타입과 일치하지 않는 옵션입니다: ${option.id}")
            }
        }
    }
}
