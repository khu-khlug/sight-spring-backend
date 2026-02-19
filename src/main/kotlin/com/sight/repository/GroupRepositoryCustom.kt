package com.sight.repository

import com.sight.domain.group.GroupOrderBy
import com.sight.repository.dto.GroupListDto

interface GroupRepositoryCustom {
    fun findGroups(
        offset: Int,
        limit: Int,
        joined: Boolean? = null,
        bookmarked: Boolean? = null,
        orderBy: GroupOrderBy? = null,
        requesterId: Long,
    ): List<GroupListDto>

    fun countGroups(
        joined: Boolean? = null,
        bookmarked: Boolean? = null,
        requesterId: Long,
    ): Long
}
