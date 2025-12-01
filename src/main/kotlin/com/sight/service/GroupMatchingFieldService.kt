package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.auth.UserRole
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.GroupMatchingField
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.service.dto.GroupMatchingFieldAnswer
import com.sight.service.dto.GroupMatchingFieldDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupMatchingFieldService(
    private val groupMatchingFieldRepository: GroupMatchingFieldRepository,
) {
    @Transactional
    fun addGroupMatchingField(fieldName: String): GroupMatchingFieldDto {
        val existing = groupMatchingFieldRepository.findByName(fieldName)

        // 활성 상태 중복
        if (existing != null && !isObsolete(existing)) {
            throw UnprocessableEntityException("이미 존재하는 관심분야 이름입니다")
        }

        // 폐기 상태 → 재활성화
        if (existing != null && isObsolete(existing)) {
            val reactivated = makeFieldActive(existing)
            return reactivated.toDto()
        }

        // 새로 생성
        val field =
            GroupMatchingField(
                id = UlidCreator.getUlid().toString(),
                name = fieldName,
            )

        val saved = groupMatchingFieldRepository.save(field)
        return saved.toDto()
    }

    private fun GroupMatchingField.toDto() =
        GroupMatchingFieldDto(
            id = id,
            name = name,
            createdAt = createdAt,
        )

    @Transactional
    fun deleteGroupMatchingField(fieldId: String) {
        val field =
            groupMatchingFieldRepository.findById(fieldId).orElseThrow {
                NotFoundException("존재하지 않는 관심분야입니다")
            }

        if (isObsolete(field)) {
            throw NotFoundException("존재하지 않는 관심분야입니다")
        }

        makeFieldObsolete(field)
    }

    fun findByName(name: String): GroupMatchingField? {
        return groupMatchingFieldRepository.findByName(name)
    }

    private fun isObsolete(field: GroupMatchingField): Boolean {
        return field.obsoletedAt != null
    }

    private fun makeFieldObsolete(field: GroupMatchingField) {
        field.obsoletedAt = LocalDateTime.now()
        groupMatchingFieldRepository.save(field)
    }

    fun getGroupMatchingFields(userRole: UserRole): List<GroupMatchingFieldAnswer> {
        val fields: List<GroupMatchingField> =
            when (userRole) {
                UserRole.USER -> groupMatchingFieldRepository.findAllByObsoletedAtIsNull()
                UserRole.MANAGER -> groupMatchingFieldRepository.findAll()
                else -> emptyList()
            }

        // 2. Entity -> Service DTO 변환
        return fields.map { field ->
            GroupMatchingFieldAnswer(
                id = field.id,
                name = field.name,
                createdAt = field.createdAt,
                obsoletedAt = field.obsoletedAt,
            )
        }
    }

    private fun makeFieldActive(field: GroupMatchingField): GroupMatchingField {
        field.obsoletedAt = null
        return groupMatchingFieldRepository.save(field)
    }
}
