package com.sight.service

import com.sight.core.exception.InternalServerErrorException
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupState
import com.sight.repository.GroupRepository
import com.sight.repository.projection.GroupListProjection
import com.sight.service.util.ByteBooleanMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class GroupListItem(
    val id: Long,
    val category: GroupCategory,
    val title: String,
    val state: GroupState,
    val countMember: Long,
    val allowJoin: Boolean,
    val createdAt: LocalDateTime,
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
        requesterId: Long?,
    ): GroupListResult {
        val groups: List<GroupListProjection>
        val count: Long

        if (bookmarked == true && requesterId != null) {
            groups = groupRepository.findBookmarkedGroups(requesterId, offset, limit)
            count = groupRepository.countBookmarkedGroups(requesterId)
        } else {
            groups = groupRepository.findAllGroups(offset, limit)
            count = groupRepository.countAllGroups()
        }

        return GroupListResult(
            count = count,
            groups = groups.map { it.toGroupListItem() },
        )
    }

    private fun GroupListProjection.toGroupListItem(): GroupListItem =
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
            allowJoin = ByteBooleanMapper.map(this.allowJoin),
            createdAt = this.createdAt,
        )
}
