package com.sight.repository

import com.sight.domain.group.Group
import com.sight.repository.projection.GroupListProjection
import com.sight.repository.projection.GroupWithMemberProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupRepository : JpaRepository<Group, Long> {
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
        JOIN khlug_group_member gm ON g.id = gm.group
        JOIN khlug_members m ON gm.member = m.id
        WHERE g.id IN :groupIds
    """,
        nativeQuery = true,
    )
    fun findGroupsWithMembers(groupIds: List<Long>): List<GroupWithMemberProjection>

    @Query(
        value = """
        SELECT
            g.id as id,
            g.category as category,
            g.title as title,
            g.state as state,
            g.count_member as countMember,
            g.allow_join as allowJoin,
            g.created_at as createdAt
        FROM khlug_group g
        ORDER BY g.created_at DESC
        LIMIT :limit OFFSET :offset
    """,
        nativeQuery = true,
    )
    fun findAllGroups(
        offset: Int,
        limit: Int,
    ): List<GroupListProjection>

    @Query(
        value = """
        SELECT COUNT(*) FROM khlug_group
    """,
        nativeQuery = true,
    )
    fun countAllGroups(): Long

    @Query(
        value = """
        SELECT
            g.id as id,
            g.category as category,
            g.title as title,
            g.state as state,
            g.count_member as countMember,
            g.allow_join as allowJoin,
            g.created_at as createdAt
        FROM khlug_group g
        JOIN khlug_group_bookmark b ON g.id = b.`group`
        WHERE b.member = :memberId
        ORDER BY g.created_at DESC
        LIMIT :limit OFFSET :offset
    """,
        nativeQuery = true,
    )
    fun findBookmarkedGroups(
        memberId: Long,
        offset: Int,
        limit: Int,
    ): List<GroupListProjection>

    @Query(
        value = """
        SELECT COUNT(*) FROM khlug_group_bookmark WHERE member = :memberId
    """,
        nativeQuery = true,
    )
    fun countBookmarkedGroups(memberId: Long): Long
}
