package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.controllers.http.dto.AddGroupMatchingFieldRequest
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.GroupMatchingField
import com.sight.repository.GroupMatchingFieldRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupMatchingFieldService(
    private val groupMatchingFieldRepository: GroupMatchingFieldRepository,
) {
    @Transactional
    fun addGroupMatchingField(request: AddGroupMatchingFieldRequest): GroupMatchingField {
        if (groupMatchingFieldRepository.existsByName(request.fieldName)) {
            throw UnprocessableEntityException("이미 존재하는 관심분야 이름입니다")
        }

        val field =
            GroupMatchingField(
                id = UlidCreator.getUlid().toString(),
                name = request.fieldName,
            )

        return groupMatchingFieldRepository.save(field)
    }

    @Transactional
    fun deleteGroupMatchingField(fieldId: String) {
        // 활성 필드만 조회
        val field =
            groupMatchingFieldRepository.findByIdAndObsoletedAtIsNull(fieldId)
                .orElseThrow { NotFoundException("존재하지 않는 관심분야입니다") }

        // soft delete: obsoletedAt 설정
        field.obsoletedAt = LocalDateTime.now()
        groupMatchingFieldRepository.save(field)
    }
}
