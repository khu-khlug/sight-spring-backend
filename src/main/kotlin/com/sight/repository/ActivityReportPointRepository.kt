package com.sight.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class ActivityReportPointRepository(private val jdbcTemplate: JdbcTemplate) {
    fun addPoints(
        memberId: Long,
        actorId: Long,
        delta: Int,
        message: String,
    ) {
        check(jdbcTemplate.update("UPDATE khlug_members SET expoint = expoint + ? WHERE id = ?", delta, memberId) == 1) {
            "활동보고 경험치를 반영할 회원을 찾을 수 없습니다."
        }
        check(
            jdbcTemplate.update(
                "INSERT INTO khlug_expoint_log (member, `from`, message, point) VALUES (?, ?, ?, ?)",
                memberId,
                actorId,
                message,
                delta,
            ) == 1,
        ) { "활동보고 경험치 이력을 저장할 수 없습니다." }
    }
}
