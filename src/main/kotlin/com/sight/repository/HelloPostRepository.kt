package com.sight.repository

import com.sight.repository.dto.HelloPostView
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class HelloPostRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findRecentPosts(limit: Int): List<HelloPostView> {
        return jdbcTemplate.query(
            """
            SELECT id, category, title, date
            FROM khlug_blog
            ORDER BY date DESC, id DESC
            LIMIT ?
            """.trimIndent(),
            { rs, _ ->
                HelloPostView(
                    id = rs.getLong("id"),
                    category = rs.getString("category"),
                    title = rs.getString("title"),
                    date = rs.getTimestamp("date").toInstant(),
                )
            },
            limit,
        )
    }
}
