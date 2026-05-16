package com.sight.repository

import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupState
import com.sight.repository.dto.GroupListDto

interface GroupRepositoryCustom {
    fun findGroups(
        offset: Int,
        limit: Int,
        joined: Boolean? = null,
        bookmarked: Boolean? = null,
        categories: List<GroupCategory>? = null,
        state: GroupState? = null,
        interest: String? = null,
        keyword: String? = null,
        orderBy: GroupOrderBy? = null,
        requesterId: Long,
    ): List<GroupListDto>

    fun countGroups(
        joined: Boolean? = null,
        bookmarked: Boolean? = null,
        categories: List<GroupCategory>? = null,
        state: GroupState? = null,
        interest: String? = null,
        keyword: String? = null,
        requesterId: Long,
    ): Long
}
