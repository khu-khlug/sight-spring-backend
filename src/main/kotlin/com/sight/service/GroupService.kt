package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.InternalServerErrorException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.GroupBookmark
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupState
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupBookmarkRepository
import com.sight.repository.GroupMemberRepository
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
    private val groupBookmarkRepository: GroupBookmarkRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val pointService: PointService,
    private val notificationService: NotificationService,
) {
    @Transactional(readOnly = true)
    fun listGroups(
        offset: Int,
        limit: Int,
        bookmarked: Boolean?,
        joined: Boolean?,
        categories: List<String>?,
        state: String?,
        interest: String?,
        keyword: String?,
        orderBy: GroupOrderBy?,
        requesterId: Long,
    ): GroupListResult {
        val validCategories =
            categories?.map { value ->
                GroupCategory.fromValue(value)
                    ?: throw BadRequestException("유효하지 않은 카테고리입니다: $value")
            }

        val validState =
            state?.let { value ->
                GroupState.fromValue(value)
                    ?: throw BadRequestException("유효하지 않은 상태입니다: $value")
            }

        val groups =
            groupRepository.findGroups(
                offset = offset,
                limit = limit,
                joined = joined,
                bookmarked = bookmarked,
                categories = validCategories,
                state = validState,
                interest = interest,
                keyword = keyword,
                orderBy = orderBy,
                requesterId = requesterId,
            )
        val count =
            groupRepository.countGroups(
                joined = joined,
                bookmarked = bookmarked,
                categories = validCategories,
                state = validState,
                interest = interest,
                keyword = keyword,
                requesterId = requesterId,
            )

        return GroupListResult(
            count = count,
            groups = groups.map { it.toGroupListItem() },
        )
    }

    @Transactional
    fun addBookmark(
        groupId: Long,
        requesterId: Long,
    ) {
        groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (groupBookmarkRepository.existsByMemberAndGroup(requesterId, groupId)) {
            throw UnprocessableEntityException("이미 즐겨찾기한 그룹입니다.")
        }
        groupBookmarkRepository.save(GroupBookmark(member = requesterId, group = groupId))
    }

    @Transactional
    fun cancelBookmark(
        groupId: Long,
        requesterId: Long,
    ) {
        groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (!groupBookmarkRepository.existsByMemberAndGroup(requesterId, groupId)) {
            throw UnprocessableEntityException("즐겨찾기 되어있지 않습니다.")
        }
        groupBookmarkRepository.deleteByMemberAndGroup(requesterId, groupId)
    }

    @Transactional
    fun publishPortfolio(
        groupId: Long,
        requesterId: Long,
    ): Boolean {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (group.master != requesterId) throw ForbiddenException("그룹장만 포트폴리오를 발행할 수 있습니다.")
        if (group.portfolio) throw BadRequestException("이미 발행된 포트폴리오입니다.")

        groupRepository.save(group.copy(portfolio = true))

        val members = groupMemberRepository.findByGroupId(groupId)
        members.forEach { member ->
            pointService.givePoint(member.member, 10, "포트폴리오 발행")
            notificationService.createNotification(
                userId = member.member,
                category = NotificationCategory.GROUP,
                title = "포트폴리오 발행",
                content = "${group.title} 그룹의 포트폴리오가 발행되었습니다.",
            )
        }

        return true
    }

    @Transactional
    fun cancelPortfolio(
        groupId: Long,
        requesterId: Long,
    ) {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (group.master != requesterId) throw ForbiddenException("그룹장만 포트폴리오를 취소할 수 있습니다.")
        if (!group.portfolio) throw NotFoundException("발행된 포트폴리오가 없습니다.")

        groupRepository.save(group.copy(portfolio = false))

        val members = groupMemberRepository.findByGroupId(groupId)
        members.forEach { member ->
            pointService.givePoint(member.member, -10, "포트폴리오 취소")
            notificationService.createNotification(
                userId = member.member,
                category = NotificationCategory.GROUP,
                title = "포트폴리오 취소",
                content = "${group.title} 그룹의 포트폴리오가 취소되었습니다.",
            )
        }
    }

    private fun GroupListDto.toGroupListItem(): GroupListItem =
        GroupListItem(
            id = this.id,
            category =
                GroupCategory.fromValue(this.category)
                    ?: throw InternalServerErrorException("알 수 없는 그룹 카테고리입니다: ${this.category}"),
            title = this.title,
            state =
                GroupState.fromValue(this.state)
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
