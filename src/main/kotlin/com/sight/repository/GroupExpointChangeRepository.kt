package com.sight.repository

import com.sight.domain.group.GroupExpointChange
import com.sight.domain.group.GroupLogType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupExpointChangeRepository : JpaRepository<GroupExpointChange, Long> {
    // 대상의 경험치 변동을 일으킨 그룹 로그 중 로그타입이 logTypes에 속하고 발생 일시가 가장 최근인 로그의 식별자를 조회한다.
    @Query(
        """
        SELECT c.groupLogId
        FROM GroupExpointChange c, GroupLog l
        WHERE c.groupLogId = l.id AND c.targetId = :targetId AND l.logType IN :logTypes
        ORDER BY l.createdAt DESC, l.id DESC
        LIMIT 1
    """,
    )
    fun findLatestGroupLogIdByTargetIdAndLogTypes(
        @Param("targetId") targetId: String,
        @Param("logTypes") logTypes: Collection<GroupLogType>,
    ): Long?

    // 한 행위로 적용한 경험치 변동. 행 수가 대상 회원 수로 제한되므로 전체 조회한다.
    fun findByGroupLogId(groupLogId: Long): List<GroupExpointChange>
}
