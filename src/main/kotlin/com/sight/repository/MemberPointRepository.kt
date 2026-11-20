package com.sight.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class MemberPointRepository(private val jdbcTemplate: JdbcTemplate) {
    fun addPoints(memberId: Long, points: Int): Int =
        jdbcTemplate.update("UPDATE khlug_members SET expoint = expoint + ? WHERE id = ?", points, memberId)

    fun recordPointChange(
        memberId: Long,
        actorMemberId: Long,
        points: Int,
        message: String,
    ) {
        jdbcTemplate.update(
            "INSERT INTO expoint_log (`member`, `from`, message, point, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
            memberId,
            actorMemberId,
            message,
            points,
        )
    }
}
