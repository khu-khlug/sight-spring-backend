package com.sight.service

import com.sight.controllers.http.dto.AddGroupMatchingFieldRequest
import com.sight.controllers.http.dto.ApproveFieldRequestResponse
import com.sight.controllers.http.dto.FieldInfo
import com.sight.controllers.http.dto.FieldRequestStatus
import com.sight.controllers.http.dto.GetFieldRequestsResponse
import com.sight.controllers.http.dto.ProcessDetails
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.groupmatching.GroupMatchingFieldRequest
import com.sight.repository.GroupMatchingFieldRequestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupMatchingFieldRequestService(
    private val groupMatchingFieldRequestRepository: GroupMatchingFieldRequestRepository,
    private val groupMatchingFieldService: GroupMatchingFieldService,
) {
    fun getAllFieldRequests(): List<GetFieldRequestsResponse> {
        val requests = groupMatchingFieldRequestRepository.findAll()
        return requests.map { it.toResponse() }
    }

    @Transactional
    fun approveFieldRequest(fieldRequestId: String): ApproveFieldRequestResponse {
        // 1. FieldRequest 조회
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

        // 3. GroupMatchingField 생성
        val createdField =
            groupMatchingFieldService.addGroupMatchingField(
                AddGroupMatchingFieldRequest(fieldName = fieldRequest.fieldName),
            )

        // 4. FieldRequest 업데이트 (approvedAt 설정)
        val now = LocalDateTime.now()
        val updatedRequest =
            fieldRequest.copy(
                approvedAt = now,
            )
        groupMatchingFieldRequestRepository.save(updatedRequest)

        // 5. Response 반환
        return ApproveFieldRequestResponse(
            field =
                FieldInfo(
                    id = createdField.id,
                    name = createdField.name,
                    createdAt = createdField.createdAt,
                ),
            approvedAt = now,
        )
    }

    private fun GroupMatchingFieldRequest.toResponse(): GetFieldRequestsResponse {
        val status =
            when {
                approvedAt != null -> FieldRequestStatus.APPROVED
                rejectedAt != null -> FieldRequestStatus.REJECTED
                else -> FieldRequestStatus.PENDING
            }

        val processDetails =
            when (status) {
                FieldRequestStatus.APPROVED ->
                    ProcessDetails(
                        processedAt = approvedAt!!,
                        rejectReason = null,
                    )
                FieldRequestStatus.REJECTED ->
                    ProcessDetails(
                        processedAt = rejectedAt!!,
                        rejectReason = rejectReason,
                    )
                FieldRequestStatus.PENDING -> null
            }

        return GetFieldRequestsResponse(
            id = id,
            fieldName = fieldName,
            requestedBy = requesterUserId,
            requestedAt = createdAt,
            requestReason = requestReason,
            status = status,
            processDetails = processDetails,
        )
    }
}
