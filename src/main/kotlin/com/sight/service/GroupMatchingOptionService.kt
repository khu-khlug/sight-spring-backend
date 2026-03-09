package com.sight.service

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
