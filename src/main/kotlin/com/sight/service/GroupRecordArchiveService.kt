package com.sight.service

import com.sight.repository.GroupRecordArchiveRepository
import com.sight.repository.dto.SavedRecordView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class SavedRecordListResult(
    val page: Int,
    val totalCount: Long,
    val receivedCount: Int,
    val records: List<SavedRecordView>,
)

@Service
class GroupRecordArchiveService(
    private val groupRecordArchiveRepository: GroupRecordArchiveRepository,
) {
    // 요청자가 모든 그룹에서 보관한 기록을 조회한다. 그룹 상태·소속 여부·원본 삭제 여부로 거르지 않는다.
    @Transactional(readOnly = true)
    fun listSavedRecords(
        memberId: Long,
        page: Int,
    ): SavedRecordListResult {
        val offset = page.toLong() * PAGE_SIZE
        val records =
            groupRecordArchiveRepository.findSavedRecordsByMemberIdOrderBySavedAtDesc(
                memberId = memberId,
                offset = offset,
                limit = PAGE_SIZE,
            )
        val totalCount = groupRecordArchiveRepository.countByMemberId(memberId)

        return SavedRecordListResult(
            page = page,
            totalCount = totalCount,
            receivedCount = records.size,
            records = records,
        )
    }

    companion object {
        // 한 번에 반환하는 최대 기록 수. 본문 전체를 담아 항목이 무거우므로 카드창 기록 목록과 같은 크기다.
        const val PAGE_SIZE: Int = 10
    }
}
