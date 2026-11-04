package com.sight.repository

import com.sight.domain.group.GroupMember
import com.sight.repository.dto.GroupMemberListDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class GroupMemberRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val groupMemberRowMapper =
        RowMapper { rs, _ ->
            GroupMember(
                groupId = rs.getLong("group_id"),
                memberId = rs.getLong("member_id"),
                joinedAt = rs.getTimestamp("joined_at").toInstant(),
            )
        }

    fun findMemberListByGroupId(groupId: Long): List<GroupMemberListDto> {
        return jdbcTemplate.query(
            """
            SELECT m.id AS user_id, m.name AS name, m.realname AS realname
            FROM khlug_group_member gm
            JOIN khlug_members m ON gm.member_id = m.id
            WHERE gm.group_id = ?
            ORDER BY m.id ASC
            """.trimIndent(),
            { rs, _ ->
                GroupMemberListDto(
                    userId = rs.getLong("user_id"),
                    name = rs.getString("name"),
                    realname = rs.getString("realname"),
                )
            },
            groupId,
        )
    }

    fun findByGroupId(groupId: Long): List<GroupMember> {
        return jdbcTemplate.query(
            "SELECT group_id, member_id, joined_at FROM khlug_group_member WHERE group_id = ?",
            groupMemberRowMapper,
            groupId,
        )
    }

    fun findByMemberId(memberId: Long): List<GroupMember> {
        return jdbcTemplate.query(
            "SELECT group_id, member_id, joined_at FROM khlug_group_member WHERE member_id = ?",
            groupMemberRowMapper,
            memberId,
        )
    }

    fun findByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): GroupMember? {
        return jdbcTemplate.query(
            "SELECT group_id, member_id, joined_at FROM khlug_group_member WHERE group_id = ? AND member_id = ?",
            groupMemberRowMapper,
            groupId,
            memberId,
        ).firstOrNull()
    }

    fun existsByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): Boolean {
        val count =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM khlug_group_member WHERE group_id = ? AND member_id = ?",
                Int::class.java,
                groupId,
                memberId,
            ) ?: 0

        return count > 0
    }

    fun save(
        groupId: Long,
        memberId: Long,
        joinedAt: Instant,
    ) {
        jdbcTemplate.update(
            "INSERT INTO khlug_group_member (group_id, member_id, joined_at) VALUES (?, ?, ?)",
            groupId,
            memberId,
            Timestamp.from(joinedAt),
        )
    }

    fun delete(
        groupId: Long,
        memberId: Long,
    ) {
        jdbcTemplate.update(
            "DELETE FROM khlug_group_member WHERE group_id = ? AND member_id = ?",
            groupId,
            memberId,
        )
    }

    fun saveAll(groupMembers: List<GroupMember>) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO khlug_group_member (group_id, member_id, joined_at) VALUES (?, ?, ?)",
            groupMembers,
            groupMembers.size,
        ) { ps, groupMember ->
            ps.setLong(1, groupMember.groupId)
            ps.setLong(2, groupMember.memberId)
            ps.setTimestamp(3, Timestamp.from(groupMember.joinedAt))
        }
    }
}
