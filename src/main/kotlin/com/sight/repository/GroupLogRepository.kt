package com.sight.repository

import com.sight.repository.dto.GroupLogListDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class GroupLogRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findLogsByGroupId(
        groupId: Long,
        offset: Int,
        limit: Int,
    ): List<GroupLogListDto> {
        return jdbcTemplate.query(
            """
            SELECT id, member, message, created_at
            FROM khlug_group_log
            WHERE `group` = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """.trimIndent(),
            { rs, _ ->
                GroupLogListDto(
                    id = rs.getLong("id"),
                    memberId = rs.getLong("member"),
                    message = rs.getString("message"),
                    createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                )
            },
            groupId,
            limit,
            offset,
        )
    }

    fun countLogsByGroupId(groupId: Long): Long {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM khlug_group_log WHERE `group` = ?",
            Long::class.java,
            groupId,
        ) ?: 0L
    }
}
