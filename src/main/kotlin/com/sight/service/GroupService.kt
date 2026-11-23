package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupBookmark
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupInterest
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupStatus
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupBookmarkRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import com.sight.repository.dto.GroupListDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class GroupLeaderInfo(
    val userId: Long,
    val name: String,
)

data class GroupListItem(
    val id: Long,
    val category: GroupCategory,
    val title: String,
    // 그룹 목록 API가 유지하는 상태 문자열. PROGRESS, SUSPEND, END_SUCCESS, END_FAIL 중 하나다.
    val state: String,
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
    private val integerIdService: IntegerIdService,
    private val groupRepository: GroupRepository,
    private val groupBookmarkRepository: GroupBookmarkRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val memberRepository: MemberRepository,
    private val notificationService: NotificationService,
    private val groupLogService: GroupLogService,
    private val pointService: PointService,
) {
    @Transactional(readOnly = true)
    fun listGroupsByQuery(
        offset: Int,
        limit: Int,
        bookmarked: Boolean?,
        joined: Boolean?,
        categories: List<String>?,
        state: String?,
        interest: String?,
        keyword: String?,
        orderBy: String?,
        requesterId: Long,
    ): GroupListResult {
        val validOrderBy =
            orderBy?.let { value ->
                GroupOrderBy.fromValue(value)
                    ?: throw BadRequestException("유효하지 않은 정렬 기준입니다: $value")
            }

        return listGroups(
            offset = offset,
            limit = limit,
            bookmarked = bookmarked,
            joined = joined,
            categories = categories,
            state = state,
            interest = interest,
            keyword = keyword,
            orderBy = validOrderBy,
            requesterId = requesterId,
        )
    }

    // 그룹 목록 API는 레거시 형식(소문자 유형, 레거시 상태 문자열, 자유 문자열 관심 분야)을 유지한다.
    // 새 그룹 도메인 값과의 변환은 이 함수에서 한다.
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
                GroupCategory.entries.find { it.name.lowercase() == value }
                    ?: throw BadRequestException("유효하지 않은 카테고리입니다: $value")
            }

        if (state != null && state !in LEGACY_STATE_VALUES) {
            throw BadRequestException("유효하지 않은 상태입니다: $state")
        }
        // 보류(pending) 상태는 새 그룹 상태에 없으므로 해당하는 그룹이 없다.
        if (state == LEGACY_PENDING_STATE) return GroupListResult(count = 0, groups = emptyList())
        val validStatus = state?.let { LEGACY_STATE_TO_STATUS.getValue(it) }

        // 관심 분야는 enum 이름으로만 검색할 수 있다. 일치하는 enum이 없으면 해당하는 그룹이 없다.
        val validInterest =
            interest?.takeIf { it.isNotBlank() }?.let { value ->
                GroupInterest.entries.find { it.name == value }
                    ?: return GroupListResult(count = 0, groups = emptyList())
            }

        val groups =
            groupRepository.findGroups(
                offset = offset,
                limit = limit,
                joined = joined,
                bookmarked = bookmarked,
                categories = validCategories,
                status = validStatus,
                interest = validInterest,
                keyword = keyword,
                orderBy = orderBy,
                requesterId = requesterId,
            )
        val count =
            groupRepository.countGroups(
                joined = joined,
                bookmarked = bookmarked,
                categories = validCategories,
                status = validStatus,
                interest = validInterest,
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
        val group = getGroupForBookmarkChange(groupId, requesterId)
        if (groupBookmarkRepository.existsByMemberIdAndGroupId(requesterId, groupId)) {
            throw UnprocessableEntityException("이미 즐겨찾기한 그룹입니다.")
        }
        groupBookmarkRepository.saveAndFlush(
            GroupBookmark(id = integerIdService.nextId(), memberId = requesterId, groupId = groupId, createdAt = Instant.now()),
        )
        notificationService.createNotification(
            userId = requesterId,
            category = NotificationCategory.GROUP,
            title = "그룹 즐겨찾기 추가",
            content = "‘${group.title}’ 그룹을 즐겨찾기에 추가했습니다.",
        )
    }

    @Transactional
    fun cancelBookmark(
        groupId: Long,
        requesterId: Long,
    ) {
        val group = getGroupForBookmarkChange(groupId, requesterId)
        if (groupBookmarkRepository.deleteByMemberIdAndGroupId(requesterId, groupId) == 0) {
            throw UnprocessableEntityException("즐겨찾기 되어있지 않습니다.")
        }
        notificationService.createNotification(
            userId = requesterId,
            category = NotificationCategory.GROUP,
            title = "그룹 즐겨찾기 해제",
            content = "‘${group.title}’ 그룹을 즐겨찾기에서 해제했습니다.",
        )
    }

    private fun getGroupForBookmarkChange(
        groupId: Long,
        requesterId: Long,
    ): Group {
        // 스냅샷 조회 전에 잠가 앞선 요청의 커밋이 중복 조회에 반영되게 한다.
        memberRepository.findForUpdateById(requesterId)
            ?: throw NotFoundException("회원을 찾을 수 없습니다.")
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (group.isStopped) throw BadRequestException("중단된 그룹은 즐겨찾기를 변경할 수 없습니다.")
        return group
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = [Exception::class])
    fun publishPortfolio(groupId: Long, requesterId: Long): Boolean {
        changePortfolioPublication(groupId, requesterId, published = true)
        return true
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = [Exception::class])
    fun cancelPortfolio(groupId: Long, requesterId: Long) {
        changePortfolioPublication(groupId, requesterId, published = false)
    }

    private fun changePortfolioPublication(groupId: Long, requesterId: Long, published: Boolean) {
        val group = getGroupForPortfolioChange(groupId, requesterId)
        if (group.isPortfolioPublished == published) throw portfolioAlreadyInState(published)

        if (groupRepository.updatePortfolioPublishedById(groupId, published, GroupStatus.PROGRESS, requesterId) == 0) {
            getGroupForPortfolioChange(groupId, requesterId)
            throw portfolioAlreadyInState(published)
        }

        val points = if (published) 10 else -10
        val title = if (published) "포트폴리오 발행" else "포트폴리오 취소"
        val action = if (published) "발행" else "취소"
        val members = groupMemberRepository.findByGroupId(groupId).sortedBy { it.memberId }
        members.forEach { member ->
            notificationService.createNotification(
                userId = member.memberId,
                category = NotificationCategory.GROUP,
                title = title,
                content = "${group.title} 그룹의 포트폴리오가 ${action}되었습니다.",
            )
        }
        groupLogService.createLog(
            groupId = groupId,
            actorMemberId = requesterId,
            data = if (published) GroupLogData.GroupPortfolioPublished else GroupLogData.GroupPortfolioUnpublished,
        )
        members.forEach { member -> pointService.givePoint(member.memberId, points, title) }
    }

    private fun getGroupForPortfolioChange(groupId: Long, requesterId: Long): Group {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (!group.isLeader(requesterId)) throw ForbiddenException("그룹장만 포트폴리오를 변경할 수 있습니다.")
        if (group.isStopped || group.isEnded) {
            throw BadRequestException("중단되거나 종료된 그룹은 포트폴리오를 변경할 수 없습니다.")
        }
        return group
    }

    private fun portfolioAlreadyInState(published: Boolean): RuntimeException =
        if (published) BadRequestException("이미 발행된 포트폴리오입니다.") else NotFoundException("발행된 포트폴리오가 없습니다.")

    private fun GroupListDto.toGroupListItem(): GroupListItem =
        GroupListItem(
            id = this.id,
            category = this.category,
            title = this.title,
            state = STATUS_TO_LEGACY_STATE_NAME.getValue(this.status),
            countMember = this.memberCount,
            allowJoin = this.allowJoin,
            createdAt = LocalDateTime.ofInstant(this.createdAt, ZoneOffset.UTC),
            leader =
                GroupLeaderInfo(
                    userId = this.leaderUserId,
                    name = this.leaderName,
                ),
        )

    companion object {
        private const val LEGACY_PENDING_STATE = "pending"

        private val LEGACY_STATE_TO_STATUS: Map<String, GroupStatus> =
            mapOf(
                "progress" to GroupStatus.PROGRESS,
                "suspend" to GroupStatus.STOP,
                "end-success" to GroupStatus.SUCCESS,
                "end-fail" to GroupStatus.FAIL,
            )

        private val LEGACY_STATE_VALUES: Set<String> = LEGACY_STATE_TO_STATUS.keys + LEGACY_PENDING_STATE

        private val STATUS_TO_LEGACY_STATE_NAME: Map<GroupStatus, String> =
            mapOf(
                GroupStatus.PROGRESS to "PROGRESS",
                GroupStatus.STOP to "SUSPEND",
                GroupStatus.SUCCESS to "END_SUCCESS",
                GroupStatus.FAIL to "END_FAIL",
            )
    }
}
