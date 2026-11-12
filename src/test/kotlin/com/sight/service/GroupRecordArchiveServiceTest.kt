package com.sight.service

import com.sight.repository.GroupRecordArchiveRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GroupRecordArchiveServiceTest {
    private val groupRecordArchiveRepository = mock<GroupRecordArchiveRepository>()
    private val groupRecordArchiveService = GroupRecordArchiveService(groupRecordArchiveRepository)

    @Test
    fun `page만큼 10개씩 건너뛴 위치부터 요청자의 보관 기록을 조회한다`() {
        given(groupRecordArchiveRepository.findSavedRecordsByMemberIdOrderBySavedAtDesc(MEMBER_ID, 10L, 10))
            .willReturn(emptyList())
        given(groupRecordArchiveRepository.countByMemberId(MEMBER_ID)).willReturn(11L)

        val result = groupRecordArchiveService.listSavedRecords(MEMBER_ID, 1)

        verify(groupRecordArchiveRepository).findSavedRecordsByMemberIdOrderBySavedAtDesc(MEMBER_ID, 10L, 10)
        assertEquals(1, result.page)
        assertEquals(11L, result.totalCount)
        assertEquals(0, result.receivedCount)
    }

    companion object {
        private const val MEMBER_ID = 2L
    }
}
