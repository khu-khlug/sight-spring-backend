package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.expoint.ExpointRule
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupLogData
import com.sight.domain.group.GroupLogExpointChange
import com.sight.domain.group.GroupLogType
import com.sight.domain.notification.NotificationCategory
import com.sight.repository.GroupMemberRepository
import com.sight.repository.GroupRepository
import com.sight.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.HtmlUtils
import java.time.Instant

data class GroupMemberListItem(
    val userId: Long,
    val name: String,
    val college: String,
    val cardCount: Long,
    val recordCount: Long,
)

data class GroupMemberListResult(
    val leaderUserId: Long?,
    val members: List<GroupMemberListItem>,
)

@Service
class GroupMemberService(
    private val integerIdService: IntegerIdService,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val memberRepository: MemberRepository,
    private val groupLogService: GroupLogService,
    private val notificationService: NotificationService,
    private val pointService: PointService,
) {
    @Transactional(readOnly = true)
    fun listGroupMembers(groupId: Long): GroupMemberListResult {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        val members = groupMemberRepository.findMemberListByGroupId(groupId)
        val leaderId = group.leaderMemberId
        val sorted =
            members
                .map { it.toItem() }
                .sortedWith(
                    compareBy<GroupMemberListItem> { if (leaderId != null && it.userId == leaderId) 0 else 1 }
                        .thenBy { it.name }
                        .thenBy { it.userId },
                )

        return GroupMemberListResult(leaderUserId = leaderId, members = sorted)
    }

    private fun com.sight.repository.dto.GroupMemberListDto.toItem(): GroupMemberListItem =
        GroupMemberListItem(
            userId = this.userId,
            name = this.name,
            college = this.college,
            cardCount = this.cardCount,
            recordCount = this.recordCount,
        )

    @Transactional
    fun delegateLeader(
        groupId: Long,
        requesterId: Long,
        newLeaderId: Long,
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        if (group.isEnded) {
            throw BadRequestException("종료된 그룹입니다")
        }

        if (newLeaderId == requesterId) {
            throw BadRequestException("자기 자신에게 위임할 수 없습니다")
        }

        val newLeader =
            memberRepository.findById(newLeaderId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, newLeaderId)) {
            throw BadRequestException("위임 대상이 그룹 멤버가 아닙니다")
        }

        val oldLeader =
            memberRepository.findById(requesterId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }

        // 그 사이 다른 요청이 그룹장을 바꿨으면 요청자는 더 이상 그룹장이 아니다.
        if (groupRepository.changeLeaderById(groupId, requesterId, newLeaderId) == 0) {
            throw ForbiddenException("그룹장만 위임할 수 있습니다")
        }
        recordGroupActivity(groupId)

        groupLogService.createLog(
            groupId = groupId,
            actorMemberId = requesterId,
            data =
                GroupLogData.GroupLeaderDelegated(
                    fromUserId = oldLeader.id,
                    fromUserName = oldLeader.realname,
                    fromUserCollege = oldLeader.college,
                    toUserId = newLeader.id,
                    toUserName = newLeader.realname,
                    toUserCollege = newLeader.college,
                ),
        )

        val notificationContent =
            "<a href=\"/group/$groupId#member\"><u>${HtmlUtils.htmlEscape(group.title)}</u></a> " +
                "그룹장이 ${newLeader.realname}에게 위임되었습니다."
        val memberIds = groupMemberRepository.findByGroupId(groupId).map { it.memberId }
        memberIds.forEach { memberId ->
            notificationService.createNotification(
                userId = memberId,
                category = NotificationCategory.GROUP,
                title = "",
                content = notificationContent,
            )
        }
    }

    @Transactional
    fun joinGroup(
        groupId: Long,
        requesterId: Long,
        isManager: Boolean = false,
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        val isMember = groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)

        if (group.category == GroupCategory.MANAGE && !isManager) {
            throw ForbiddenException("운영 그룹은 운영진만 참가할 수 있습니다")
        }

        if (!group.allowJoin) {
            throw ForbiddenException("이 그룹은 참여가 제한되어 있습니다")
        }

        if (group.isEnded) {
            throw BadRequestException("참여할 수 없는 그룹 상태입니다")
        }

        if (isMember) {
            throw BadRequestException("이미 그룹 멤버입니다")
        }

        val groupMemberId = integerIdService.nextId()
        groupMemberRepository.save(groupMemberId, groupId, requesterId, Instant.now())

        val point = ExpointRule.그룹참여.point
        groupLogService.createLog(
            groupId = groupId,
            actorMemberId = requesterId,
            data = GroupLogData.GroupMemberJoined,
            expointChange =
                GroupLogExpointChange(
                    targetId = groupMemberId.toString(),
                    point = point,
                    memberIds = listOf(requesterId),
                ),
        )

        val escapedTitle = HtmlUtils.htmlEscape(group.title)
        val notificationContent =
            "<a href=\"/group/$groupId\"><u>$escapedTitle</u></a> 그룹에 참여했습니다."

        notificationService.createNotification(
            userId = requesterId,
            category = NotificationCategory.GROUP,
            title = "",
            content = notificationContent,
        )

        val additionalRecipients =
            if (group.category == GroupCategory.MANAGE) {
                groupMemberRepository.findByGroupId(groupId).map { it.memberId }
            } else {
                listOfNotNull(group.leaderMemberId)
            }
        additionalRecipients.forEach { memberId ->
            notificationService.createNotification(
                userId = memberId,
                category = NotificationCategory.GROUP,
                title = "",
                content = notificationContent,
            )
        }

        pointService.givePoint(
            targetUserId = requesterId,
            point = point,
            message = "<u>$escapedTitle</u> 그룹에 참여했습니다.",
        )

        groupRepository.addMemberCountById(groupId, 1)
        recordGroupActivity(groupId)
    }

    @Transactional
    fun kickMember(
        groupId: Long,
        requesterId: Long,
        kickedMemberId: Long,
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        if (group.isEnded) {
            throw BadRequestException("종료된 그룹입니다")
        }

        if (group.category == GroupCategory.MANAGE) {
            throw BadRequestException("운영 그룹에서는 멤버를 내보낼 수 없습니다")
        }

        if (kickedMemberId == requesterId) {
            throw BadRequestException("자기 자신을 내보낼 수 없습니다")
        }

        val kickedMember =
            memberRepository.findById(kickedMemberId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }

        val targetMember =
            groupMemberRepository.findByGroupIdAndMemberId(groupId, kickedMemberId)
                ?: throw BadRequestException("내보낼 대상이 그룹 멤버가 아닙니다")

        // 동시에 같은 회원을 내보내면 한 요청만 삭제에 성공하고, 나머지는 경험치·로그를 다시 남기지 않는다.
        if (groupMemberRepository.delete(groupId, kickedMemberId) == 0) {
            throw BadRequestException("내보낼 대상이 그룹 멤버가 아닙니다")
        }

        val escapedTitle = HtmlUtils.htmlEscape(group.title)
        groupLogService.revertExpointChange(
            groupId = groupId,
            actorMemberId = requesterId,
            targetId = targetMember.id.toString(),
            targetLogTypes = setOf(GroupLogType.GROUP_MEMBER_JOINED),
            revertLog =
                GroupLogData.GroupMemberKicked(
                    targetUserId = kickedMember.id,
                    targetUserName = kickedMember.realname,
                    targetUserCollege = kickedMember.college,
                ),
            message = "<u>$escapedTitle</u> 그룹에서 내보내졌습니다.",
            fallbackRule = ExpointRule.그룹탈퇴,
            fallbackMemberIds = listOf(kickedMemberId),
        )

        notificationService.createNotification(
            userId = kickedMemberId,
            category = NotificationCategory.GROUP,
            title = "",
            content = "<a href=\"/group/$groupId\"><u>$escapedTitle</u></a> 그룹에서 내보내졌습니다.",
        )

        groupRepository.addMemberCountById(groupId, -1)
        recordGroupActivity(groupId)
    }

    @Transactional
    fun leaveGroup(
        groupId: Long,
        requesterId: Long,
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        if (group.isEnded) {
            throw BadRequestException("종료된 그룹입니다")
        }

        // 운영 카테고리가 아닌 그룹에서 그룹장은 탈퇴 불가 (먼저 위임 필요)
        if (group.isLeader(requesterId) && group.category != GroupCategory.MANAGE) {
            throw BadRequestException("그룹장은 다른 멤버에게 그룹장을 위임한 후 탈퇴할 수 있습니다")
        }

        val groupMember =
            groupMemberRepository.findByGroupIdAndMemberId(groupId, requesterId)
                ?: throw BadRequestException("그룹 멤버가 아닙니다")

        // 동시에 나가기를 요청하면 한 요청만 삭제에 성공하고, 나머지는 경험치·로그를 다시 남기지 않는다.
        if (groupMemberRepository.delete(groupId, requesterId) == 0) {
            throw BadRequestException("그룹 멤버가 아닙니다")
        }

        // 운영 카테고리 그룹에서 그룹장이 탈퇴한 경우: 남은 멤버 중 한 명을 새 그룹장으로 지정, 없으면 null 저장
        if (group.isLeader(requesterId) && group.category == GroupCategory.MANAGE) {
            val remainingMembers = groupMemberRepository.findByGroupId(groupId)
            if (remainingMembers.isNotEmpty()) {
                val newLeaderId = remainingMembers.first().memberId
                groupRepository.changeLeaderById(groupId, requesterId, newLeaderId)
            } else {
                groupRepository.clearLeaderById(groupId)
            }
        }

        val escapedTitle = HtmlUtils.htmlEscape(group.title)
        groupLogService.revertExpointChange(
            groupId = groupId,
            actorMemberId = requesterId,
            targetId = groupMember.id.toString(),
            targetLogTypes = setOf(GroupLogType.GROUP_MEMBER_JOINED),
            revertLog = GroupLogData.GroupMemberLeft,
            message = "<u>$escapedTitle</u> 그룹에서 나갔습니다.",
            fallbackRule = ExpointRule.그룹탈퇴,
            fallbackMemberIds = listOf(requesterId),
        )

        val notificationContent =
            "<a href=\"/group/$groupId\"><u>$escapedTitle</u></a> 그룹에서 나갔습니다."

        notificationService.createNotification(
            userId = requesterId,
            category = NotificationCategory.GROUP,
            title = "",
            content = notificationContent,
        )

        val additionalRecipients =
            if (group.category == GroupCategory.MANAGE) {
                groupMemberRepository.findByGroupId(groupId).map { it.memberId }
            } else {
                listOfNotNull(group.leaderMemberId)
            }
        additionalRecipients.forEach { memberId ->
            notificationService.createNotification(
                userId = memberId,
                category = NotificationCategory.GROUP,
                title = "",
                content = notificationContent,
            )
        }

        groupRepository.addMemberCountById(groupId, -1)
        recordGroupActivity(groupId)
    }

    // 그룹원 변경은 활동이므로 중단 상태를 해제하고 최근 활동 일시를 갱신한다.
    // 그룹 엔티티를 통째로 저장하면 동시에 바뀐 그룹원 수를 덮어쓰므로 필요한 컬럼만 갱신한다.
    private fun recordGroupActivity(groupId: Long) {
        groupRepository.resumeStoppedById(groupId)
        groupRepository.recordActivityById(groupId, Instant.now())
    }
}
