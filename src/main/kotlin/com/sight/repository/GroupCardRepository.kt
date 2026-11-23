package com.sight.repository

import com.sight.domain.group.GroupCard
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface GroupCardRepository : JpaRepository<GroupCard, Long> {
    // 그룹의 삭제되지 않은 카드(비활성 포함)를 카드 수정 시각 내림차순(같으면 아이디 내림차순)으로 조회한다.
    fun findByGroupIdAndDeletedAtIsNullOrderByUpdatedAtDescIdDesc(
        groupId: Long,
        pageable: Pageable,
    ): List<GroupCard>
}
