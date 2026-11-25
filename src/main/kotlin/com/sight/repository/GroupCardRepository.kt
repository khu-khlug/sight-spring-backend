package com.sight.repository

import com.sight.domain.group.GroupCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupCardRepository : JpaRepository<GroupCard, Long> {
    @Query("select c from GroupCard c where c.id = :id and c.groupId = :groupId and c.deletedAt is null")
    fun findAvailableByIdAndGroupId(
        @Param("id") id: Long,
        @Param("groupId") groupId: Long,
    ): GroupCard?

    @Query("select count(c) > 0 from GroupCard c where c.id = :id and c.groupId = :groupId and c.deletedAt is not null")
    fun existsDeletedByIdAndGroupId(
        @Param("id") id: Long,
        @Param("groupId") groupId: Long,
    ): Boolean
}
