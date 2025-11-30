package com.sight.service

import com.sight.controllers.http.dto.AddGroupMatchingFieldRequest
import com.sight.controllers.http.dto.FieldRequestStatus
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.NotFoundException
import com.sight.domain.groupmatching.GroupMatchingField
import com.sight.domain.groupmatching.GroupMatchingFieldRequest
import com.sight.repository.GroupMatchingFieldRequestRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.Optional

class GroupMatchingFieldRequestServiceTest {
    private val repository = mock<GroupMatchingFieldRequestRepository>()
    private val groupMatchingFieldService = mock<GroupMatchingFieldService>()
    private val service = GroupMatchingFieldRequestService(repository, groupMatchingFieldService)

    @Test
    fun `getAllFieldRequests는 빈 목록을 반환한다`() {
        // given
        given(repository.findAll()).willReturn(emptyList())

        // when
        val result = service.getAllFieldRequests()

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `getAllFieldRequests는 PENDING 요청을 올바르게 매핑한다`() {
        // given
        val request =
            GroupMatchingFieldRequest(
                id = "req-1",
                requesterUserId = 1L,
                fieldName = "백엔드",
                requestReason = "필요해서",
                approvedAt = null,
                rejectedAt = null,
                rejectReason = null,
            )
        given(repository.findAll()).willReturn(listOf(request))

        // when
        val result = service.getAllFieldRequests()

        // then
        assertEquals(1, result.size)
        assertEquals(FieldRequestStatus.PENDING, result[0].status)
        assertNull(result[0].processDetails)
    }

    @Test
    fun `getAllFieldRequests는 APPROVED 요청을 올바르게 매핑한다`() {
        // given
        val approvedAt = LocalDateTime.now()
        val request =
            GroupMatchingFieldRequest(
                id = "req-1",
                requesterUserId = 1L,
                fieldName = "백엔드",
                requestReason = "필요해서",
                approvedAt = approvedAt,
                rejectedAt = null,
                rejectReason = null,
            )
        given(repository.findAll()).willReturn(listOf(request))

        // when
        val result = service.getAllFieldRequests()

        // then
        assertEquals(1, result.size)
        assertEquals(FieldRequestStatus.APPROVED, result[0].status)
        assertNotNull(result[0].processDetails)
        assertEquals(approvedAt, result[0].processDetails!!.processedAt)
        assertNull(result[0].processDetails!!.rejectReason)
    }

    @Test
    fun `getAllFieldRequests는 REJECTED 요청을 올바르게 매핑한다`() {
        // given
        val rejectedAt = LocalDateTime.now()
        val request =
            GroupMatchingFieldRequest(
                id = "req-1",
                requesterUserId = 1L,
                fieldName = "백엔드",
                requestReason = "필요해서",
                approvedAt = null,
                rejectedAt = rejectedAt,
                rejectReason = "중복됨",
            )
        given(repository.findAll()).willReturn(listOf(request))

        // when
        val result = service.getAllFieldRequests()

        // then
        assertEquals(1, result.size)
        assertEquals(FieldRequestStatus.REJECTED, result[0].status)
        assertNotNull(result[0].processDetails)
        assertEquals(rejectedAt, result[0].processDetails!!.processedAt)
        assertEquals("중복됨", result[0].processDetails!!.rejectReason)
    }

    @Test
    fun `approveFieldRequest는 필드 요청을 성공적으로 승인한다`() {
        // given
        val fieldRequestId = "req-1"
        val fieldName = "New Field"
        val request =
            GroupMatchingFieldRequest(
                id = fieldRequestId,
                requesterUserId = 1L,
                fieldName = fieldName,
                requestReason = "Reason",
            )
        val createdField =
            GroupMatchingField(
                id = "field-1",
                name = fieldName,
            )

        given(repository.findById(fieldRequestId)).willReturn(Optional.of(request))
        given(groupMatchingFieldService.addGroupMatchingField(any())).willReturn(createdField)

        // when
        val result = service.approveFieldRequest(fieldRequestId)

        // then
        assertEquals(createdField.id, result.field.id)
        assertEquals(createdField.name, result.field.name)
        assertNotNull(result.approvedAt)
        verify(repository).save(any())
        verify(groupMatchingFieldService).addGroupMatchingField(
            com.sight.controllers.http.dto.AddGroupMatchingFieldRequest(fieldName),
        )
    }

    @Test
    fun `approveFieldRequest는 존재하지 않는 요청이면 NotFoundException을 던진다`() {
        // given
        val fieldRequestId = "non-existent"
        given(repository.findById(fieldRequestId)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            service.approveFieldRequest(fieldRequestId)
        }
    }

    @Test
    fun `approveFieldRequest는 이미 승인된 요청이면 BadRequestException을 던진다`() {
        // given
        val fieldRequestId = "req-1"
        val request =
            GroupMatchingFieldRequest(
                id = fieldRequestId,
                requesterUserId = 1L,
                fieldName = "Field",
                requestReason = "Reason",
                approvedAt = LocalDateTime.now(),
            )
        given(repository.findById(fieldRequestId)).willReturn(Optional.of(request))

        // when & then
        assertThrows<BadRequestException> {
            service.approveFieldRequest(fieldRequestId)
        }
    }

    @Test
    fun `approveFieldRequest는 이미 거절된 요청이면 BadRequestException을 던진다`() {
        // given
        val fieldRequestId = "req-1"
        val request =
            GroupMatchingFieldRequest(
                id = fieldRequestId,
                requesterUserId = 1L,
                fieldName = "Field",
                requestReason = "Reason",
                rejectedAt = LocalDateTime.now(),
            )
        given(repository.findById(fieldRequestId)).willReturn(Optional.of(request))

        // when & then
        assertThrows<BadRequestException> {
            service.approveFieldRequest(fieldRequestId)
        }
    }
}
