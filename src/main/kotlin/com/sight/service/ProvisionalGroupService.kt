package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.groupmatching.ProvisionalGroup
import com.sight.repository.GroupMatchingAnswerRepository
import com.sight.repository.GroupMatchingRepository
import com.sight.repository.ProvisionalGroupRepository
import com.sight.service.dto.ListProvisionalGroupsResult
import com.sight.service.dto.ProvisionalGroupDetail
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProvisionalGroupService(
    private val provisionalGroupRepository: ProvisionalGroupRepository,
    private val groupMatchingRepository: GroupMatchingRepository,
    private val answerRepository: GroupMatchingAnswerRepository,
) {
    @Transactional(readOnly = true)
    fun listProvisionalGroups(groupMatchingId: String): ListProvisionalGroupsResult {
        validateGroupMatchingExists(groupMatchingId)

        val groups = provisionalGroupRepository.findAllByGroupMatchingId(groupMatchingId)
        val details = groups.map { toDetail(it) }

        return ListProvisionalGroupsResult(provisionalGroups = details)
    }

    @Transactional
    fun createProvisionalGroup(
        groupMatchingId: String,
        name: String,
    ): ProvisionalGroupDetail {
        validateGroupMatchingExists(groupMatchingId)

        val provisionalGroup =
            ProvisionalGroup(
                id = UlidCreator.getUlid().toString(),
                name = name,
                groupMatchingId = groupMatchingId,
            )
        val saved = provisionalGroupRepository.save(provisionalGroup)

        return toDetail(saved)
    }

    @Transactional
    fun updateProvisionalGroup(
        groupMatchingId: String,
        provisionalGroupId: String,
        name: String,
    ): ProvisionalGroupDetail {
        validateGroupMatchingExists(groupMatchingId)

        val provisionalGroup = findProvisionalGroup(provisionalGroupId)

        if (provisionalGroup.groupMatchingId != groupMatchingId) {
            throw BadRequestException("해당 그룹 매칭에 속하지 않는 가상 그룹입니다.")
        }

        val updated = provisionalGroup.copy(name = name)
        val saved = provisionalGroupRepository.save(updated)

        return toDetail(saved)
    }

    @Transactional
    fun deleteProvisionalGroup(
        groupMatchingId: String,
        provisionalGroupId: String,
    ) {
        validateGroupMatchingExists(groupMatchingId)

        val provisionalGroup = findProvisionalGroup(provisionalGroupId)

        if (provisionalGroup.groupMatchingId != groupMatchingId) {
            throw BadRequestException("해당 그룹 매칭에 속하지 않는 가상 그룹입니다.")
        }

        answerRepository.updateProvisionalGroupIdToNullByProvisionalGroupId(provisionalGroupId)
        provisionalGroupRepository.delete(provisionalGroup)
    }

    @Transactional
    fun assignProvisionalGroup(
        groupMatchingId: String,
        answerId: String,
        provisionalGroupId: String?,
    ) {
        validateGroupMatchingExists(groupMatchingId)

        val answer =
            answerRepository.findById(answerId)
                .orElseThrow { NotFoundException("해당 답변을 찾을 수 없습니다.") }

        if (answer.groupMatchingId != groupMatchingId) {
            throw BadRequestException("해당 그룹 매칭에 속하지 않는 답변입니다.")
        }

        if (provisionalGroupId != null) {
            val provisionalGroup = findProvisionalGroup(provisionalGroupId)
            if (provisionalGroup.groupMatchingId != groupMatchingId) {
                throw BadRequestException("해당 그룹 매칭에 속하지 않는 가상 그룹입니다.")
            }
        }

        val updated = answer.copy(provisionalGroupId = provisionalGroupId)
        answerRepository.save(updated)
    }

    private fun validateGroupMatchingExists(groupMatchingId: String) {
        if (!groupMatchingRepository.existsById(groupMatchingId)) {
            throw NotFoundException("해당 그룹 매칭을 찾을 수 없습니다.")
        }
    }

    private fun findProvisionalGroup(provisionalGroupId: String): ProvisionalGroup {
        return provisionalGroupRepository.findById(provisionalGroupId)
            .orElseThrow { NotFoundException("해당 가상 그룹을 찾을 수 없습니다.") }
    }

    private fun toDetail(provisionalGroup: ProvisionalGroup): ProvisionalGroupDetail {
        val answerCount = answerRepository.countByProvisionalGroupId(provisionalGroup.id)
        return ProvisionalGroupDetail(
            id = provisionalGroup.id,
            name = provisionalGroup.name,
            groupMatchingId = provisionalGroup.groupMatchingId,
            answerCount = answerCount,
            createdAt = provisionalGroup.createdAt,
        )
    }
}
