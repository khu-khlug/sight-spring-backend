package com.sight.service

import com.sight.core.exception.InternalServerErrorException
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupState
import com.sight.repository.GroupRepository
import com.sight.repository.dto.GroupListDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class GroupLeaderInfo(
    val userId: Long,
    val name: String,
)

data class GroupListItem(
    val id: Long,
    val category: GroupCategory,
    val title: String,
    val state: GroupState,
    val countMember: Long,
    val allowJoin: Boolean,
    val createdAt: LocalDateTime,
    val leader: GroupLeaderInfo,
)

data class GroupListResult(
    val count: Long,
    val groups: List<GroupListItem>,
)

@Service
class GroupService(
    private val groupRepository: GroupRepository,
) {
    @Transactional(readOnly = true)
    fun listGroups(
        offset: Int,
        limit: Int,
        bookmarked: Boolean?,
        joined: Boolean?,
        orderBy: GroupOrderBy?,
        requesterId: Long,
    ): GroupListResult {
        val groups = groupRepository.findGroups(offset, limit, joined, bookmarked, orderBy, requesterId)
        val count = groupRepository.countGroups(joined, bookmarked, requesterId)

        return GroupListResult(
            count = count,
            groups = groups.map { it.toGroupListItem() },
        )
    }

    private fun GroupListDto.toGroupListItem(): GroupListItem =
        GroupListItem(
            id = this.id,
            category =
                GroupCategory.entries.firstOrNull { it.value == this.category }
                    ?: throw InternalServerErrorException("알 수 없는 그룹 카테고리입니다: ${this.category}"),
            title = this.title,
            state =
                GroupState.entries.firstOrNull { it.value == this.state }
                    ?: throw InternalServerErrorException("알 수 없는 그룹 상태입니다: ${this.state}"),
            countMember = this.countMember,
            allowJoin = this.allowJoin,
            createdAt = this.createdAt,
            leader =
                GroupLeaderInfo(
                    userId = this.leaderUserId,
                    name = this.leaderName,
                ),
        )
}
