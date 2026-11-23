package com.sight.repository

import com.sight.repository.dto.GroupSearchRecordView

interface GroupSearchRepository {
    // 삭제되지 않은 카드에 속한 삭제되지 않은 기록을 기록 수정 시각 내림차순(같으면 아이디 내림차순)으로 조회한다.
    // 본문 텍스트는 저장 형식별 추출이 필요하므로 일치 여부는 호출한 쪽이 판정한다.
    //
    // 기록은 엔티티가 아니라 View로 받는다. 엔티티로 읽으면 영속성 컨텍스트가 트랜잭션이 끝날 때까지 본문(최대 1MB)을 모두 쥐고,
    // 기록은 카드 연관 매핑 없이 카드 아이디만 가지고 있어 그룹 소속을 판단하려면 카드와 조인해야 한다.
    fun findSearchableRecordsOrderByLatest(
        groupId: Long,
        offset: Long,
        limit: Int,
    ): List<GroupSearchRecordView>
}
