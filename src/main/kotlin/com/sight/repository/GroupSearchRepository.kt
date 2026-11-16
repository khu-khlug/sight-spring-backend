package com.sight.repository

import com.sight.domain.group.GroupSearchKeywords
import com.sight.repository.dto.GroupSearchCardView
import com.sight.repository.dto.GroupSearchRecordView

interface GroupSearchRepository {
    // 삭제되지 않은 카드(비활성 포함) 중 카드명에 모든 검색어가 들어 있는 카드를 카드 수정 시각 내림차순(같으면 아이디 내림차순)으로 조회한다.
    fun findCardsByTitleOrderByLatest(
        groupId: Long,
        keywords: GroupSearchKeywords,
        offset: Long,
        limit: Int,
    ): List<GroupSearchCardView>

    fun countCardsByTitle(
        groupId: Long,
        keywords: GroupSearchKeywords,
    ): Long

    // 삭제되지 않은 카드(비활성 포함) 중 카드 생성자명에 모든 검색어가 들어 있는 카드를 카드 수정 시각 내림차순(같으면 아이디 내림차순)으로 조회한다.
    fun findCardsByAuthorNameOrderByLatest(
        groupId: Long,
        keywords: GroupSearchKeywords,
        offset: Long,
        limit: Int,
    ): List<GroupSearchCardView>

    fun countCardsByAuthorName(
        groupId: Long,
        keywords: GroupSearchKeywords,
    ): Long

    // 삭제되지 않은 카드에 속한 삭제되지 않은 기록을 기록 수정 시각 내림차순(같으면 아이디 내림차순)으로 조회한다.
    // 본문 텍스트는 저장 형식별 추출이 필요하므로 일치 여부는 호출한 쪽이 판정한다.
    fun findSearchableRecordsOrderByLatest(
        groupId: Long,
        offset: Long,
        limit: Int,
    ): List<GroupSearchRecordView>

    // 삭제되지 않은 카드의 리스트·카드 정보를 조회한다. 다른 그룹의 카드와 삭제된 카드는 제외한다.
    fun findCardViewsByIds(
        groupId: Long,
        cardIds: Collection<Long>,
    ): List<GroupSearchCardView>
}
