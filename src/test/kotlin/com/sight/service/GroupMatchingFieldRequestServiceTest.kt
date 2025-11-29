package com.sight.service

import com.sight.controllers.http.dto.FieldRequestStatus
import com.sight.core.exception.UnprocessableEntityException
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
    fun `createGroupMatchingFieldRequest는 이미 등록된 관심분야 이름일 경우 예외를 발생시킨다`() {
        // given
        val requesterUserId = 100L
        val fieldName = "백엔드_개발"
        val requestReason = "관심분야 추가 요청"

        given(fieldRepository.existsByName(fieldName)).willReturn(true)
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
}
