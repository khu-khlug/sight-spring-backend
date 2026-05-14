package com.sight.repository

import com.sight.domain.group.GroupOrderBy
import com.sight.repository.dto.GroupListDto
import com.sight.repository.dto.GroupLogListDto

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

    fun findGroupLogsByGroupId(
        groupId: Long,
        offset: Int,
        limit: Int,
    ): List<GroupLogListDto>

    fun countGroupLogsByGroupId(groupId: Long): Long

    fun insertGroupLog(
        id: Long,
        groupId: Long,
        memberId: Long,
        message: String,
    )

    fun incrementCountMember(groupId: Long)

    fun touchChangedAtAndPromoteFromSuspend(groupId: Long)
}
