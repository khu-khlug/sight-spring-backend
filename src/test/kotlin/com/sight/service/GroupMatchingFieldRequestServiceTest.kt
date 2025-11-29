package com.sight.service

import com.sight.controllers.http.dto.CreateGroupMatchingFieldRequestRequest
import com.sight.controllers.http.dto.FieldRequestStatus
import com.sight.domain.groupmatching.GroupMatchingFieldRequest
import com.sight.repository.GroupMatchingFieldRequestRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime

class GroupMatchingFieldRequestServiceTest {
    private val repository = mock<GroupMatchingFieldRequestRepository>()
    private val service = GroupMatchingFieldRequestService(repository)

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
        val requesterId = 100L
        val request =
            CreateGroupMatchingFieldRequestRequest(
                fieldName = "백엔드_개발",
                requestReason = "관심분야 추가 요청",
            )

        given(repository.save(any<GroupMatchingFieldRequest>())).willAnswer {
            it.arguments[0] as GroupMatchingFieldRequest
        }

        // when
        val result = service.createGroupMatchingFieldRequest(request, requesterId)

        // then
        assertEquals(request.fieldName, result.fieldName)
        assertEquals(request.requestReason, result.requestReason)
        assertEquals(requesterId, result.requesterUserId)

        // save가 1번 호출되었는지 검증
        verify(repository).save(any<GroupMatchingFieldRequest>())
    }

    @Test
    fun `createGroupMatchingFieldRequest는 사유가 null이면 빈 문자열로 저장한다`() {
        // given
        val requesterId = 200L
        val request =
            CreateGroupMatchingFieldRequestRequest(
                fieldName = "프론트엔드",
                requestReason = null,
            )

        given(repository.save(any<GroupMatchingFieldRequest>())).willAnswer {
            it.arguments[0] as GroupMatchingFieldRequest
        }

        // when
        val result = service.createGroupMatchingFieldRequest(request, requesterId)

        // then
        assertEquals(request.fieldName, result.fieldName)
        assertEquals("", result.requestReason) // null -> "" 변환 로직 검증

        verify(repository).save(any<GroupMatchingFieldRequest>())
    }
}
