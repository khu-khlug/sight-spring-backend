package com.sight.repository

import com.sight.domain.group.Group
import com.sight.repository.projection.GroupWithMemberProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupRepository : JpaRepository<Group, Long>, GroupRepositoryCustom {
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
}
