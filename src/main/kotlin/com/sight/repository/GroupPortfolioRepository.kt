package com.sight.repository

import com.sight.domain.group.GroupCard
import com.sight.repository.projection.GroupPortfolioCountProjection
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

// 그룹 포트폴리오에 포함된 카드를 조회한다. 포트폴리오는 별도 테이블 없이 카드의 포함 여부로 정해진다.
interface GroupPortfolioRepository : Repository<GroupCard, Long> {
    // 포트폴리오에 포함된 삭제되지 않은 카드 수와, 그런 카드가 하나 이상 있는 리스트 수를 한 번에 센다.
    @Query(
        """
        SELECT COUNT(c) AS cardCount, COUNT(DISTINCT c.listId) AS listCount
        FROM GroupCard c
        WHERE c.groupId = :groupId AND c.deletedAt IS NULL AND c.isInPortfolio = true
        """,
    )
    fun countPortfolioCardsAndListsByGroupId(
        @Param("groupId") groupId: Long,
    ): GroupPortfolioCountProjection
}
