package com.sight.service

import com.sight.controllers.http.dto.FieldRequestStatus
import com.sight.controllers.http.dto.GetFieldRequestsResponse
import com.sight.controllers.http.dto.ProcessDetails
import com.sight.domain.groupmatching.GroupMatchingFieldRequest
import com.sight.repository.GroupMatchingFieldRequestRepository
import org.springframework.stereotype.Service

@Service
class GroupMatchingFieldRequestService(
    private val groupMatchingFieldRequestRepository: GroupMatchingFieldRequestRepository,
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
}
