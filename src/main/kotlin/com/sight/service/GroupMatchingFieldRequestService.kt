package com.sight.service

import com.github.f4b6a3.ulid.UlidCreator
import com.sight.controllers.http.dto.FieldRequestStatus
import com.sight.controllers.http.dto.GetFieldRequestsResponse
import com.sight.controllers.http.dto.ProcessDetails
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.GroupMatchingFieldRequest
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingFieldRequestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GroupMatchingFieldRequestService(
    private val groupMatchingFieldRequestRepository: GroupMatchingFieldRequestRepository,
    private val groupMatchingFieldRepository: GroupMatchingFieldRepository,
) {
    fun getAllFieldRequests(): List<GetFieldRequestsResponse> {
        val requests = groupMatchingFieldRequestRepository.findAll()
        return requests.map { it.toResponse() }
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

    @Transactional
    fun createGroupMatchingFieldRequest(
        fieldName: String,
        requestReason: String,
        requesterUserId: Long,
    ): GroupMatchingFieldRequest {
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
