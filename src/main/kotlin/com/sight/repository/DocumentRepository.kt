package com.sight.repository

import com.sight.domain.document.Document
import com.sight.repository.projection.TalkWithAuthorProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<Document, Long> {
    @Query(
        value = """
        SELECT
            d.id as id,
            d.title as title,
            d.author as authorId,
            m.realname as authorRealname,
            d.created_at as createdAt
        FROM khlug_document d
        JOIN khlug_members m ON d.author = m.id
        WHERE d.board = :board AND d.state = :state
        ORDER BY d.created_at DESC
        LIMIT :limit OFFSET :offset
    """,
        nativeQuery = true,
    )
    fun findTalksWithAuthor(
        board: Long,
        state: String,
        offset: Int,
        limit: Int,
    ): List<TalkWithAuthorProjection>

    fun countByBoardAndState(
        board: Long,
        state: String,
    ): Long
}
