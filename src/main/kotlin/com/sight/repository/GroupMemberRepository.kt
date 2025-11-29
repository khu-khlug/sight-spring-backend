package com.sight.repository

import com.sight.domain.group.GroupMember
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class GroupMemberRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findByGroupId(groupId: Long): List<GroupMember> {
        return jdbcTemplate.query(
            "SELECT `group`, member FROM khlug_group_member WHERE `group` = ?",
            { rs, _ ->
                GroupMember(
                    group = rs.getLong("group"),
                    member = rs.getLong("member"),
                )
            },
            groupId,
        )
    }

    fun findByMemberId(memberId: Long): List<GroupMember> {
        return jdbcTemplate.query(
            "SELECT `group`, member FROM khlug_group_member WHERE member = ?",
            { rs, _ ->
                GroupMember(
                    group = rs.getLong("group"),
                    member = rs.getLong("member"),
                )
            },
            memberId,
        )
    }

    fun findByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): GroupMember? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT `group`, member FROM khlug_group_member WHERE `group` = ? AND member = ?",
                { rs, _ ->
                    GroupMember(
                        group = rs.getLong("group"),
                        member = rs.getLong("member"),
                    )
                },
                groupId,
                memberId,
            )
        } catch (e: Exception) {
            null
        }
    }

    fun existsByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): Boolean {
        val count =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM khlug_group_member WHERE `group` = ? AND member = ?",
                Int::class.java,
                groupId,
                memberId,
            ) ?: 0

        return count > 0
    }

    fun save(
        groupId: Long,
        memberId: Long,
    ) {
        jdbcTemplate.update(
            "INSERT INTO khlug_group_member (`group`, member) VALUES (?, ?)",
            groupId,
            memberId,
        )
    }

    fun saveAll(groupMembers: List<GroupMember>) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO khlug_group_member (`group`, member) VALUES (?, ?)",
            groupMembers,
            groupMembers.size,
        ) { ps, groupMember ->
            ps.setLong(1, groupMember.group)
            ps.setLong(2, groupMember.member)
        }
    }
}
