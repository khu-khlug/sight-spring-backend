package com.sight.repository

import com.sight.domain.group.GroupBookmark
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupBookmarkRepository : JpaRepository<GroupBookmark, Long> {
    fun existsByMemberIdAndGroupId(
        memberId: Long,
        groupId: Long,
    ): Boolean

    @Modifying
    @Query("DELETE FROM GroupBookmark b WHERE b.memberId = :memberId AND b.groupId = :groupId")
    fun deleteByMemberIdAndGroupId(
        @Param("memberId") memberId: Long,
        @Param("groupId") groupId: Long,
    ): Int
}
