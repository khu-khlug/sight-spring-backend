package com.sight.repository

import com.sight.domain.ideacloud.IdeaCloud
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface IdeaCloudRepository : JpaRepository<IdeaCloud, Long> {
    @Query(
        value = "SELECT * FROM khlug_ideacloud WHERE state = 'public' ORDER BY RAND() LIMIT 1",
        nativeQuery = true,
    )
    fun findRandomPublicIdea(): IdeaCloud?
}
