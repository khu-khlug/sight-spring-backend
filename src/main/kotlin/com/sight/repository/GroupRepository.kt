package com.sight.repository

import com.sight.domain.group.Group
import com.sight.domain.group.GroupStatus
import com.sight.repository.projection.GroupWithMemberProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface GroupRepository : JpaRepository<Group, Long>, GroupRepositoryCustom {
    // 그룹원 수를 원자적으로 증감한다. 읽은 값에 더해 저장하지 않으므로 동시 참가·탈퇴에도 개수가 유실되지 않는다.
    @Modifying
    @Query("UPDATE KhlugGroup g SET g.memberCount = g.memberCount + :delta WHERE g.id = :id")
    fun addMemberCountById(
        @Param("id") id: Long,
        @Param("delta") delta: Long,
    ): Int

    // 중단 상태인 그룹만 진행 중으로 바꾼다. 활동에 따른 중단 해제에 쓴다.
    @Modifying
    @Query("UPDATE KhlugGroup g SET g.status = :progress WHERE g.id = :id AND g.status = :stopped")
    fun resumeStoppedById(
        @Param("id") id: Long,
        @Param("stopped") stopped: GroupStatus = GroupStatus.STOP,
        @Param("progress") progress: GroupStatus = GroupStatus.PROGRESS,
    ): Int

    // 최근 활동 일시를 더 늦은 시각으로만 갱신한다.
    @Modifying
    @Query("UPDATE KhlugGroup g SET g.lastActivityAt = :at WHERE g.id = :id AND g.lastActivityAt < :at")
    fun recordActivityById(
        @Param("id") id: Long,
        @Param("at") at: Instant,
    ): Int

    // 현재 그룹장이 요청자일 때만 그룹장을 바꾼다. 바뀐 행 수가 0이면 이미 다른 요청이 그룹장을 바꾼 것이다.
    @Modifying
    @Query(
        "UPDATE KhlugGroup g SET g.leaderMemberId = :newLeaderMemberId " +
            "WHERE g.id = :id AND g.leaderMemberId = :currentLeaderMemberId",
    )
    fun changeLeaderById(
        @Param("id") id: Long,
        @Param("currentLeaderMemberId") currentLeaderMemberId: Long,
        @Param("newLeaderMemberId") newLeaderMemberId: Long,
    ): Int

    // 포트폴리오 발행 여부가 반대 상태일 때만 바꾼다. 바뀐 행 수가 0이면 이미 같은 상태로 바뀐 것이다.
    @Modifying
    @Query(
        "UPDATE KhlugGroup g SET g.isPortfolioPublished = :published " +
            "WHERE g.id = :id AND g.isPortfolioPublished <> :published",
    )
    fun updatePortfolioPublishedById(
        @Param("id") id: Long,
        @Param("published") published: Boolean,
    ): Int

    @Query(
        value = """
        SELECT
            g.id as groupId,
            g.title as groupTitle,
            g.created_at as groupCreatedAt,
            m.id as memberId,
            m.name as memberName,
            m.realname as memberRealName,
            m.number as memberNumber
        FROM khlug_group g
        JOIN khlug_group_member gm ON g.id = gm.group_id
        JOIN khlug_members m ON gm.member_id = m.id
        WHERE g.id IN :groupIds
    """,
        nativeQuery = true,
    )
    fun findGroupsWithMembers(groupIds: List<Long>): List<GroupWithMemberProjection>
}
