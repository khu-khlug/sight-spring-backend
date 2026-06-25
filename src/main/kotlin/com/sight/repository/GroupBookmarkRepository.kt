package com.sight.repository

import com.sight.domain.group.GroupBookmark
import com.sight.domain.group.GroupBookmarkId
import org.springframework.data.jpa.repository.JpaRepository

interface GroupBookmarkRepository : JpaRepository<GroupBookmark, GroupBookmarkId> {
    fun existsByMemberAndGroup(
        member: Long,
        group: Long,
    ): Boolean

    fun deleteByMemberAndGroup(
        member: Long,
        group: Long,
    )
}
