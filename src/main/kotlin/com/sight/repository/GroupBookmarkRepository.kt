package com.sight.repository

import com.sight.domain.group.GroupBookmark
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBookmarkRepository : JpaRepository<GroupBookmark, Long> {
    fun existsByMemberIdAndGroupId(
        memberId: Long,
        groupId: Long,
    ): Boolean

    fun deleteByMemberIdAndGroupId(
        memberId: Long,
        groupId: Long,
    )
}
