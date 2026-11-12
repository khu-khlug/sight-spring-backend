package com.sight.repository

import com.sight.repository.dto.SavedRecordView

interface GroupRecordArchiveRepositoryCustom {
    // 회원이 보관한 기록을 모든 그룹에 걸쳐 보관 일시 내림차순(같으면 기록 아이디 내림차순)으로 조회한다.
    // 원본 기록·소속 카드가 삭제된 보관도 포함한다.
    fun findSavedRecordsByMemberIdOrderBySavedAtDesc(
        memberId: Long,
        offset: Long,
        limit: Int,
    ): List<SavedRecordView>
}
