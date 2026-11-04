package com.sight.service

import com.sight.core.exception.BadRequestException
import com.sight.core.exception.ForbiddenException
import com.sight.core.exception.NotFoundException
import com.sight.domain.group.Group
import com.sight.domain.group.GroupCategory
import com.sight.domain.group.GroupLogType
import com.sight.domain.group.GroupPolicy
import com.sight.domain.group.GroupVisibility
import com.sight.domain.member.Member
import com.sight.domain.member.StudentStatus
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
    val realname: String,
    val isLeader: Boolean,
)

data class GroupMemberListResult(
    val members: List<GroupMemberListItem>,
)

@Service
class GroupMemberService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val memberRepository: MemberRepository,
    private val groupLogService: GroupLogService,
    private val notificationService: NotificationService,
    private val pointService: PointService,
) {
    @Transactional(readOnly = true)
    fun listGroupMembers(
        groupId: Long,
        requesterId: Long,
    ): GroupMemberListResult {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        val requester =
            memberRepository.findById(requesterId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }

        val isMember = groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)
        if (!canViewGroup(group, requester, isMember)) {
            throw ForbiddenException("해당 그룹의 멤버 목록을 조회할 권한이 없습니다")
        }

        val members = groupMemberRepository.findMemberListByGroupId(groupId)
        val sorted =
            members
                .map { it.toItem(group) }
                .sortedWith(compareByDescending<GroupMemberListItem> { it.isLeader }.thenBy { it.userId })

        return GroupMemberListResult(members = sorted)
    }

    private fun canViewGroup(
        group: Group,
        requester: Member,
        isMember: Boolean,
    ): Boolean =
        when (group.visibility) {
            GroupVisibility.PRIVATE -> isMember || group.authorMemberId == requester.id
            GroupVisibility.MANAGER_ONLY -> requester.manager
            GroupVisibility.MEMBER_ONLY -> requester.studentStatus != StudentStatus.UNITED
            GroupVisibility.PUBLIC -> true
        }

    private fun com.sight.repository.dto.GroupMemberListDto.toItem(group: Group): GroupMemberListItem =
        GroupMemberListItem(
            userId = this.userId,
            name = this.name,
            realname = this.realname,
            isLeader = group.isLeader(this.userId),
        )

    @Transactional
    fun delegateMaster(
        groupId: Long,
        requesterId: Long,
        newMasterId: Long,
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        if (!group.isLeader(requesterId)) {
            throw ForbiddenException("그룹장만 위임할 수 있습니다")
        }

        if (newMasterId == requesterId) {
            throw BadRequestException("자기 자신에게 위임할 수 없습니다")
        }

        val newMaster =
            memberRepository.findById(newMasterId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, newMasterId)) {
            throw BadRequestException("위임 대상이 그룹 멤버가 아닙니다")
        }

        val oldMaster =
            memberRepository.findById(requesterId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }

        group.changeLeader(newMasterId)
        saveWithActivity(group)

        groupLogService.createLog(
            groupId = groupId,
            actorMemberId = requesterId,
            logType = GroupLogType.GROUP_LEADER_DELEGATED,
            logData =
                mapOf(
                    "fromUserId" to oldMaster.id,
                    "fromUserName" to oldMaster.realname,
                    "fromUserCollege" to oldMaster.college,
                    "toUserId" to newMaster.id,
                    "toUserName" to newMaster.realname,
                    "toUserCollege" to newMaster.college,
                ),
        )

        val notificationContent =
            "<a href=\"/group/$groupId#member\"><u>${HtmlUtils.htmlEscape(group.title)}</u></a> " +
                "그룹장이 ${newMaster.realname}에게 위임되었습니다."
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
    ) {
        val group =
            groupRepository.findById(groupId).orElseThrow {
                NotFoundException("그룹을 찾을 수 없습니다")
            }

        val requester =
            memberRepository.findById(requesterId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }
        val isMember = groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)

        if (!canViewGroup(group, requester, isMember)) {
            throw ForbiddenException("그룹에 참여할 수 있는 권한이 없습니다")
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

        groupMemberRepository.save(groupId, requesterId, Instant.now())

        groupLogService.createLog(
            groupId = groupId,
            actorMemberId = requesterId,
            logType = GroupLogType.GROUP_MEMBER_JOINED,
            logData = emptyMap(),
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

        if (groupId != GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID) {
            pointService.givePoint(
                targetUserId = requesterId,
                point = 40,
                message = "<u>$escapedTitle</u> 그룹에 참여했습니다.",
            )
        }

        group.increaseMemberCount()
        saveWithActivity(group)
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

        if (group.category != GroupCategory.MANAGE && !group.isLeader(requesterId)) {
            throw ForbiddenException("그룹장만 멤버를 내보낼 수 있습니다")
        }

        if (kickedMemberId == requesterId) {
            throw BadRequestException("자기 자신을 내보낼 수 없습니다")
        }

        val kickedMember =
            memberRepository.findById(kickedMemberId).orElseThrow {
                NotFoundException("회원을 찾을 수 없습니다")
            }

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, kickedMemberId)) {
            throw BadRequestException("내보낼 대상이 그룹 멤버가 아닙니다")
        }

        groupMemberRepository.delete(groupId, kickedMemberId)

        groupLogService.createLog(
            groupId = groupId,
            actorMemberId = requesterId,
            logType = GroupLogType.GROUP_MEMBER_KICKED,
            logData =
                mapOf(
                    "targetUserId" to kickedMember.id,
                    "targetUserName" to kickedMember.realname,
                    "targetUserCollege" to kickedMember.college,
                ),
        )

        val escapedTitle = HtmlUtils.htmlEscape(group.title)
        notificationService.createNotification(
            userId = kickedMemberId,
            category = NotificationCategory.GROUP,
            title = "",
            content = "<a href=\"/group/$groupId\"><u>$escapedTitle</u></a> 그룹에서 내보내졌습니다.",
        )

        if (groupId != GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID) {
            pointService.givePoint(
                targetUserId = kickedMemberId,
                point = -40,
                message = "<u>$escapedTitle</u> 그룹에서 내보내졌습니다.",
            )
        }

        group.decreaseMemberCount()
        saveWithActivity(group)
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

        if (!groupMemberRepository.existsByGroupIdAndMemberId(groupId, requesterId)) {
            throw BadRequestException("그룹 멤버가 아닙니다")
        }

        // 운영 카테고리가 아닌 그룹에서 그룹장은 탈퇴 불가 (먼저 위임 필요)
        if (group.isLeader(requesterId) && group.category != GroupCategory.MANAGE) {
            throw BadRequestException("그룹장은 다른 멤버에게 그룹장을 위임한 후 탈퇴할 수 있습니다")
        }

        groupMemberRepository.delete(groupId, requesterId)

        groupLogService.createLog(
            groupId = groupId,
            actorMemberId = requesterId,
            logType = GroupLogType.GROUP_MEMBER_LEFT,
            logData = emptyMap(),
        )

        val escapedTitle = HtmlUtils.htmlEscape(group.title)
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

        if (groupId != GroupPolicy.EXPOINT_EXCLUDED_GROUP_ID) {
            pointService.givePoint(
                targetUserId = requesterId,
                point = -40,
                message = "<u>$escapedTitle</u> 그룹에서 나갔습니다.",
            )
        }

        group.decreaseMemberCount()
        saveWithActivity(group)
    }

    // 그룹원 변경은 활동이므로 중단 상태를 해제하고 최근 활동 일시를 갱신해 저장한다.
    private fun saveWithActivity(group: Group) {
        group.resumeByActivity()
        group.recordActivity(Instant.now())
        groupRepository.save(group)
    }
}
