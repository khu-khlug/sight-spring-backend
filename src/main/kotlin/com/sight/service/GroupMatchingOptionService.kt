package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.groupmatching.GroupMatchingOption
import com.sight.domain.groupmatching.GroupMatchingType
import com.sight.repository.GroupMatchingOptionRepository
import com.sight.repository.GroupMatchingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupMatchingOptionService(
    private val groupMatchingOptionRepository: GroupMatchingOptionRepository,
    private val groupMatchingRepository: GroupMatchingRepository,
) {
    @Transactional(readOnly = true)
    fun listOptionsByQuery(
        groupMatchingId: String,
        type: String,
    ): List<GroupMatchingOption> = listOptions(groupMatchingId, type.toGroupMatchingType())

    @Transactional(readOnly = true)
    fun listOptions(
        groupMatchingId: String,
        type: GroupMatchingType,
    ): List<GroupMatchingOption> {
        groupMatchingRepository.findById(groupMatchingId)
            .orElseThrow { NotFoundException("해당 그룹 매칭을 찾을 수 없습니다") }

        return groupMatchingOptionRepository.findAllByGroupMatchingIdAndGroupMatchingType(
            groupMatchingId,
            type,
        )
    }
}

private fun String.toGroupMatchingType(): GroupMatchingType =
    runCatching { GroupMatchingType.valueOf(this.uppercase()) }
        .getOrElse { throw BadRequestException("유효하지 않은 그룹 매칭 유형입니다: $this") }
