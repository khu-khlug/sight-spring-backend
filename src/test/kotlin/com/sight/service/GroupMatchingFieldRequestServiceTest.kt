package com.sight.service

import com.sight.controllers.http.dto.FieldRequestStatus
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.groupmatching.GroupMatchingField
import com.sight.domain.groupmatching.GroupMatchingFieldRequest
import com.sight.repository.GroupMatchingFieldRepository
import com.sight.repository.GroupMatchingFieldRequestRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.Optional

class GroupMatchingFieldRequestServiceTest {
    private val repository = mock<GroupMatchingFieldRequestRepository>()
    private val fieldRepository = mock<GroupMatchingFieldRepository>()
    private val service = GroupMatchingFieldRequestService(repository, fieldRepository)

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
    fun `createGroupMatchingFieldRequest는 요청을 받아 성공적으로 저장한다`() {
        // given
        val requesterUserId = 100L
        val fieldName = "백엔드_개발"
        val requestReason = "관심분야 추가 요청"

        // 중복이 없음을 가정
        given(fieldRepository.existsByName(fieldName)).willReturn(false)
        given(repository.existsByFieldName(fieldName)).willReturn(false)

        given(repository.save(any<GroupMatchingFieldRequest>())).willAnswer {
            it.arguments[0] as GroupMatchingFieldRequest
        }

        // when
        val result = service.createGroupMatchingFieldRequest(fieldName, requestReason, requesterUserId)

        // then
        assertEquals(fieldName, result.fieldName)
        assertEquals(requestReason, result.requestReason)
        assertEquals(requesterUserId, result.requesterUserId)

        // save가 1번 호출되었는지 검증
        verify(repository).save(any<GroupMatchingFieldRequest>())
    }

    @Test
    fun `createGroupMatchingFieldRequest는 이미 등록되고 obsoletedAt이 null인 관심분야 이름일 경우 예외를 발생시킨다`() {
        // given
        val requesterUserId = 100L
        val fieldName = "백엔드_개발"
        val requestReason = "관심분야 추가 요청"

        val activeField = mock<GroupMatchingField>()
        given(activeField.obsoletedAt).willReturn(null)

        given(fieldRepository.findByName(fieldName)).willReturn(activeField)
        given(repository.existsByFieldName(fieldName)).willReturn(false)

        // when & then
        assertThrows<UnprocessableEntityException> {
            service.createGroupMatchingFieldRequest(
                fieldName = fieldName,
                requestReason = requestReason,
                requesterUserId = requesterUserId,
            )
        }

        // 예외가 발생했으므로 저장 로직은 호출되지 않아야 함
        verify(repository, never()).save(any())
    }

    @Test
    fun `createGroupMatchingFieldRequest는 폐기된(ObsoletedAt != null) 관심분야 이름일 경우 정상적으로 저장한다`() {
        // given
        val requesterUserId = 100L
        val fieldName = "구_백엔드" // 예전에 쓰다 버린 이름
        val requestReason = "다시 쓰고 싶어요"

        // Mock: 이미 존재하지만 폐기된 필드 (obsoletedAt != null)
        val obsoletedField = mock<GroupMatchingField>()
        given(obsoletedField.obsoletedAt).willReturn(LocalDateTime.now()) // 삭제됨

        // findByName 호출 시 폐기된 필드 반환 -> 서비스는 이를 '없는 것'과 동일하게 취급해야 함
        given(fieldRepository.findByName(fieldName)).willReturn(obsoletedField)

        given(repository.existsByFieldName(fieldName)).willReturn(false)
        given(repository.save(any<GroupMatchingFieldRequest>())).willAnswer {
            it.arguments[0] as GroupMatchingFieldRequest
        }

        // when
        val result = service.createGroupMatchingFieldRequest(fieldName, requestReason, requesterUserId)

        // then
        // 예외 없이 저장 성공 확인
        assertEquals(fieldName, result.fieldName)
        verify(repository).save(any<GroupMatchingFieldRequest>())
    }

    @Test
    fun `createGroupMatchingFieldRequest는 이미 요청된 이름일 경우 예외를 발생시킨다`() {
        // given
        val requesterUserId = 100L
        val fieldName = "프론트엔드_요청"
        val requestReason = "관심분야 추가 요청"

        given(fieldRepository.existsByName(fieldName)).willReturn(false)
        given(repository.existsByFieldName(fieldName)).willReturn(true)

        // when & then
        assertThrows<UnprocessableEntityException> {
            service.createGroupMatchingFieldRequest(
                fieldName = fieldName,
                requestReason = requestReason,
                requesterUserId = requesterUserId,
            )
        }

        // 예외가 발생했으므로 저장 로직은 호출되지 않아야 함
        verify(repository, never()).save(any())
    }

    @Test
    fun `rejectGroupMatchingFieldRequest는 존재하는 요청을 거절하고 거절 정보를 저장해야 한다`() {
        // given
        val fieldRequestId = "req-123"
        val rejectReason = "중복된 관심분야입니다"
        val existingRequest =
            GroupMatchingFieldRequest(
                id = fieldRequestId,
                requesterUserId = 1L,
                fieldName = "백엔드",
                requestReason = "필요해서",
                approvedAt = null,
                rejectedAt = null,
                rejectReason = null,
            )

        given(repository.findById(fieldRequestId)).willReturn(Optional.of(existingRequest))
        given(repository.save(any<GroupMatchingFieldRequest>())).willAnswer {
            it.arguments[0] as GroupMatchingFieldRequest
        }

        // when
        val result = service.rejectGroupMatchingFieldRequest(fieldRequestId, rejectReason)

        // then
        assertNotNull(result.rejectedAt)
        assertEquals(rejectReason, result.rejectReason)
        verify(repository).save(any<GroupMatchingFieldRequest>())
    }

    @Test
    fun `rejectGroupMatchingFieldRequest는 존재하지 않는 요청이면 예외를 발생시켜야 한다`() {
        // given
        val fieldRequestId = "not-exists"
        val rejectReason = "거절 사유"

        given(repository.findById(fieldRequestId)).willReturn(Optional.empty())

        // when & then
        assertThrows<NotFoundException> {
            service.rejectGroupMatchingFieldRequest(fieldRequestId, rejectReason)
        }

        verify(repository, never()).save(any())
    }
}
