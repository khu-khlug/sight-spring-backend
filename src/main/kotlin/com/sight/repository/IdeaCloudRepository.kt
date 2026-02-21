package com.sight.repository

import com.sight.domain.ideacloud.IdeaCloud
import com.sight.repository.projection.IdeaCloudWithAuthorProjection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface IdeaCloudRepository : JpaRepository<IdeaCloud, Long> {
    @Query(
        value = "SELECT * FROM khlug_ideacloud WHERE state = 'public' ORDER BY RAND() LIMIT 1",
        nativeQuery = true,
    )
    fun findRandomPublicIdea(): IdeaCloud?

    @Query(
        value = """
            SELECT i.id as id, i.idea as idea, i.author as authorId, m.realname as authorName, i.created_at as createdAt
            FROM khlug_ideacloud i
            JOIN khlug_members m ON i.author = m.id
            WHERE i.state = 'public'
            ORDER BY RAND()
            LIMIT 5
        """,
        nativeQuery = true,
    )
    fun findRandomPublicIdeasWithAuthor(): List<IdeaCloudWithAuthorProjection>
}
