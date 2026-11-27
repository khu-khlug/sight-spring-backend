package com.sight.service

import com.sight.core.auth.Requester
import com.sight.core.auth.UserRole
import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.core.exception.UnprocessableEntityException
import com.sight.core.groupauth.GroupVisibility
import com.sight.domain.group.GroupBookmark
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupInfoItem
import com.sight.domain.group.GroupInterest
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupOrderBy
import com.sight.domain.group.GroupStatus
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupBookmarkRepository
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.dto.GroupListDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.HtmlUtils
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
    private val pointService: PointService,
    private val notificationService: NotificationService,
    private val groupLogService: GroupLogService,
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
        groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (groupBookmarkRepository.existsByMemberIdAndGroupId(requesterId, groupId)) {
            throw UnprocessableEntityException("이미 즐겨찾기한 그룹입니다.")
        }
        groupBookmarkRepository.save(
            GroupBookmark(id = integerIdService.nextId(), memberId = requesterId, groupId = groupId, createdAt = Instant.now()),
        )
    }

    @Transactional
    fun cancelBookmark(
        groupId: Long,
        requesterId: Long,
    ) {
        groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (!groupBookmarkRepository.existsByMemberIdAndGroupId(requesterId, groupId)) {
            throw UnprocessableEntityException("즐겨찾기 되어있지 않습니다.")
        }
        groupBookmarkRepository.deleteByMemberIdAndGroupId(requesterId, groupId)
    }

    @Transactional
    fun publishPortfolio(groupId: Long): Boolean {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (group.isPortfolioPublished) throw BadRequestException("이미 발행된 포트폴리오입니다.")

        // 동시에 발행하면 한 요청만 상태를 바꾸고, 나머지는 경험치·알림을 다시 보내지 않는다.
        if (groupRepository.updatePortfolioPublishedById(groupId, published = true) == 0) {
            throw BadRequestException("이미 발행된 포트폴리오입니다.")
        }

        val members = groupMemberRepository.findByGroupId(groupId)
        members.forEach { member ->
            pointService.givePoint(member.memberId, 10, "포트폴리오 발행")
            notificationService.createNotification(
                userId = member.memberId,
                category = NotificationCategory.GROUP,
                title = "포트폴리오 발행",
                content = "${group.title} 그룹의 포트폴리오가 발행되었습니다.",
            )
        }

        return true
    }

    @Transactional
    fun cancelPortfolio(groupId: Long) {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }
        if (!group.isPortfolioPublished) throw NotFoundException("발행된 포트폴리오가 없습니다.")

        // 동시에 취소하면 한 요청만 상태를 바꾸고, 나머지는 경험치·알림을 다시 보내지 않는다.
        if (groupRepository.updatePortfolioPublishedById(groupId, published = false) == 0) {
            throw NotFoundException("발행된 포트폴리오가 없습니다.")
        }

        val members = groupMemberRepository.findByGroupId(groupId)
        members.forEach { member ->
            pointService.givePoint(member.memberId, -10, "포트폴리오 취소")
            notificationService.createNotification(
                userId = member.memberId,
                category = NotificationCategory.GROUP,
                title = "포트폴리오 취소",
                content = "${group.title} 그룹의 포트폴리오가 취소되었습니다.",
            )
        }
    }

    // 그룹 정보를 요청 값으로 모두 덮어쓴다. 정보 변경은 활동으로 인정하지 않으므로
    // 최근 활동 일시를 갱신하지 않고 중단 상태를 해제하지 않는다(중단 상태에서는 실행 자체를 거부한다).
    @Transactional
    fun updateGroup(
        groupId: Long,
        requester: Requester,
        title: String,
        description: String?,
        category: GroupCategory,
        interests: List<GroupInterest>?,
        techStack: String?,
        repositoryUrls: String?,
        allowJoin: Boolean,
        visibility: GroupVisibility,
    ) {
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("그룹을 찾을 수 없습니다.") }

        // 중단·종료된 그룹은 정보를 바꿀 수 없다. 중단 상태는 정보 변경이 중단을 해제하지 않으므로 거부한다.
        if (group.status != GroupStatus.PROGRESS) {
            throw BadRequestException("진행 중인 그룹만 정보를 변경할 수 있습니다.")
        }

        // 운영진 전용 유형과 운영진 공개 범위는 운영진만 지정할 수 있다. 운영진 공개가 운영 그룹에서만 가능한지는 도메인이 검사한다.
        if (requester.role != UserRole.MANAGER && (category.isManagerOnly || visibility == GroupVisibility.MANAGER_ONLY)) {
            throw ForbiddenException("운영진만 지정할 수 있는 그룹 유형 또는 공개 범위입니다.")
        }

        // 도메인은 값이 올바르지 않으면 상태를 바꾸기 전에 IllegalArgumentException을 던진다.
        val changedItems =
            try {
                group.updateInfo(
                    title = title,
                    description = description,
                    category = category,
                    interests = interests,
                    techStack = techStack,
                    repositoryUrls = repositoryUrls,
                    allowJoin = allowJoin,
                    visibility = visibility,
                )
            } catch (e: IllegalArgumentException) {
                throw BadRequestException(e.message ?: "그룹 정보가 올바르지 않습니다.")
            }

        // 현재와 같은 값으로 요청하면 성공으로 응답하되 알림·로그를 남기지 않는다.
        if (changedItems.isEmpty()) return

        val changedNames = GroupInfoItem.entries.filter { it in changedItems }.map { it.displayName }
        groupLogService.createLog(
            groupId = groupId,
            actorMemberId = requester.userId,
            data = GroupLogData.GroupInfoUpdated(changedFields = changedNames),
        )

        // 알림의 그룹 이름은 변경 후 이름이다.
        val notificationContent =
            "<a href=\"/group/$groupId\"><u>${HtmlUtils.htmlEscape(group.title)}</u></a> " +
                "그룹 정보가 변경되었습니다. (변경 항목: ${changedNames.joinToString(", ")})"
        groupMemberRepository.findByGroupId(groupId).forEach { member ->
            notificationService.createNotification(
                userId = member.memberId,
                category = NotificationCategory.GROUP,
                title = "",
                content = notificationContent,
            )
        }
    }

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
