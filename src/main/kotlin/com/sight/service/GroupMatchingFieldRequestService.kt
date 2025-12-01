package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.GroupMatchingField
import com.sight.domain.groupmatching.GroupMatchingFieldRequest
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingFieldRequestRepository
import com.sight.service.dto.GroupMatchingFieldDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupMatchingFieldRequestService(
    private val groupMatchingFieldRequestRepository: GroupMatchingFieldRequestRepository,
    private val groupMatchingFieldRepository: GroupMatchingFieldRepository,
) {
    fun getAllFieldRequests(): List<GroupMatchingFieldRequest> {
        return groupMatchingFieldRequestRepository.findAll()
    }

    @Transactional
    fun approveFieldRequest(fieldRequestId: String): GroupMatchingFieldDto {
        // 1. FieldRequest 조회
        // TODO: 동시성 처리 (규모가 커지거나 문제 발생 빈도가 높아지면 Lock 사용)
        val fieldRequest =
            groupMatchingFieldRequestRepository.findById(fieldRequestId).orElseThrow {
                NotFoundException("Field request not found")
            }

        // 2. 이미 처리된 요청인지 확인
        if (fieldRequest.approvedAt != null) {
            throw BadRequestException("이미 승인된 요청입니다")
        }
        if (fieldRequest.rejectedAt != null) {
            throw BadRequestException("이미 거절된 요청입니다")
        }

        // 3. 필드명 유효성 검증
        val trimmedFieldName = fieldRequest.fieldName.trim()
        if (trimmedFieldName.isBlank()) {
            throw BadRequestException("필드명이 비어있습니다")
        }
        if (trimmedFieldName.length > 100) {
            throw BadRequestException("필드명이 너무 깁니다")
        }

        // 4. GroupMatchingField 생성 (Repository 직접 사용)
        val existing = groupMatchingFieldRepository.findByName(trimmedFieldName)

        // 활성 상태 중복
        if (existing != null && existing.obsoletedAt == null) {
            throw UnprocessableEntityException("이미 존재하는 관심분야 이름입니다")
        }

        val createdField: GroupMatchingField =
            if (existing != null && existing.obsoletedAt != null) {
                // 폐기 상태 → 재활성화
                existing.obsoletedAt = null
                groupMatchingFieldRepository.save(existing)
            } else {
                // 새로 생성
                val field =
                    GroupMatchingField(
                        id = com.github.f4b6a3.ulid.UlidCreator.getUlid().toString(),
                        name = trimmedFieldName,
                    )
                groupMatchingFieldRepository.save(field)
            }

        // 5. FieldRequest 업데이트 (approvedAt 설정)
        val now = LocalDateTime.now()
        val updatedRequest =
            fieldRequest.copy(
                approvedAt = now,
            )
        groupMatchingFieldRequestRepository.save(updatedRequest)

        // 6. Service DTO 반환
        return GroupMatchingFieldDto(
            id = createdField.id,
            name = createdField.name,
            createdAt = createdField.createdAt,
        )
    }

    @Transactional
    fun createGroupMatchingFieldRequest(
        fieldName: String,
        requestReason: String,
        requesterUserId: Long,
    ): GroupMatchingFieldRequest {
        // Repository를 직접 사용하여 필드 존재 여부 확인
        val existingField = groupMatchingFieldRepository.findByName(fieldName)
        if (existingField != null && existingField.obsoletedAt == null) {
            throw UnprocessableEntityException("이미 등록된 관심분야 이름입니다.")
        }
        if (groupMatchingFieldRequestRepository.existsByFieldName(fieldName)) {
            throw UnprocessableEntityException("이미 승인 대기 중인 요청이 존재합니다.")
        }
        val fieldRequest =
            GroupMatchingFieldRequest(
                id = UlidCreator.getUlid().toString(),
                fieldName = fieldName,
                requestReason = requestReason,
                requesterUserId = requesterUserId,
            )

        return groupMatchingFieldRequestRepository.save(fieldRequest)
    }

    @Transactional
    fun rejectGroupMatchingFieldRequest(
        id: String,
        rejectReason: String,
    ): GroupMatchingFieldRequest {
        val fieldRequest =
            groupMatchingFieldRequestRepository.findById(id).orElseThrow {
                NotFoundException("관심분야 추가 요청을 찾을 수 없습니다.")
            }

        val rejectedRequest =
            fieldRequest.copy(
                rejectedAt = LocalDateTime.now(),
                rejectReason = rejectReason,
            )

        return groupMatchingFieldRequestRepository.save(rejectedRequest)
    }
}
