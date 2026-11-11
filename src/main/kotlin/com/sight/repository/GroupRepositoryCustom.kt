package com.sight.repository

import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupInterest
import com.sight.domain.group.GroupLog
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupStatus
import com.sight.repository.dto.GroupListDto

interface GroupRepositoryCustom {
    fun findGroups(
        offset: Int,
        limit: Int,
        joined: Boolean? = null,
        bookmarked: Boolean? = null,
        categories: List<GroupCategory>? = null,
        status: GroupStatus? = null,
        interest: GroupInterest? = null,
        keyword: String? = null,
        orderBy: GroupOrderBy? = null,
        requesterId: Long,
    ): List<GroupListDto>

    fun countGroups(
        joined: Boolean? = null,
        bookmarked: Boolean? = null,
        categories: List<GroupCategory>? = null,
        status: GroupStatus? = null,
        interest: GroupInterest? = null,
        keyword: String? = null,
        requesterId: Long,
    ): Long

    // 그룹 활동 로그를 최신순(발생 일시 내림차순, 같으면 아이디 내림차순)으로 조회한다.
    fun findGroupLogsByGroupIdOrderByLatest(
        groupId: Long,
        offset: Long,
        limit: Int,
    ): List<GroupLog>

    fun countGroupLogsByGroupId(groupId: Long): Long
}
